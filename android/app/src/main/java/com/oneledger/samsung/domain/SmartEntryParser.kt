package com.oneledger.samsung.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Serializable
data class ParsedEntry(
    val type: String = "expense", // "expense" | "income"
    val description: String = "日常消費", // 單一正規化描述
    val amount: Double = 0.0,
    val category: String = "其他",
    val date: String = LocalDate.now().toString(),
    val paymentMethod: String = "現金",
    val isForeignTx: Boolean = false,
)

/**
 * 結合 Gemini LLM 與結構化本地文法解析引擎：
 * - 優先呼叫 Gemini API 產生乾淨單一繁體中文 description
 * - 無金鑰或離線時採用「商戶優先＋完整詞彙保護」結構化解析，徹底解決先前字典截斷「摩斯漢堡→漢堡」的脆弱問題。
 */
object SmartEntryParser {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val s2tPairs = listOf(
        "汉堡" to "漢堡", "蛋饼" to "蛋餅", "铁板面" to "鐵板麵", "牛肉面" to "牛肉麵",
        "拉面" to "拉麵", "面线" to "麵線", "便当" to "便當", "饭团" to "飯糰",
        "饮料" to "飲料", "鸡腿" to "雞腿", "烧肉" to "燒肉", "火锅" to "火鍋",
        "计程车" to "計程車", "捷运" to "捷運", "高铁" to "高鐵", "全联" to "全聯",
        "买" to "買", "块" to "塊", "钱" to "錢", "消费" to "消費", "缴费" to "繳費",
        "电影" to "電影", "奖金" to "獎金", "薪资" to "薪資", "回馈" to "回饋",
        "转账" to "轉帳", "悠游卡" to "悠遊卡", "之类的" to "之類的", "然后" to "然後", "刚刚" to "剛剛",
    )

    private val knownMerchants = listOf(
        "摩斯漢堡", "麥當勞", "肯德基", "星巴克", "統一超商", "7-ELEVEN", "全家便利商店", "全家",
        "全聯福利中心", "全聯", "好市多", "Costco", "家樂福", "屈臣氏", "康是美", "無印良品",
        "優衣庫", "UNIQLO", "台灣中油", "中油", "清心福全", "五十嵐", "迷客夏", "Uber Eats",
        "Foodpanda", "Netflix", "Spotify", "YouTube", "ChatGPT", "Apple", "蝦皮", "momo",
    )

    private val knownFoodItems = listOf(
        "排骨便當", "雞腿便當", "珍珠奶茶", "鐵板麵", "牛肉麵", "滷肉飯", "蘿蔔糕", "三明治",
        "烏龍綠茶", "冰美式", "冰拿鐵", "漢堡", "蛋餅", "奶茶", "紅茶", "綠茶", "豆漿",
        "咖啡", "拿鐵", "美式", "吐司", "飯糰", "包子", "薯餅", "便當", "拉麵", "麵線",
        "水餃", "鍋貼", "炒飯", "火鍋", "燒肉", "沙拉", "牛奶", "雞蛋", "茶葉蛋",
    )

    fun toTraditional(raw: String): String {
        var out = raw
        for ((s, t) in s2tPairs) {
            if (out.contains(s)) out = out.replace(s, t)
        }
        return out
    }

    suspend fun parse(rawText: String, geminiApiKey: String, sourceApp: String = ""): ParsedEntry {
        val local = parseLocally(rawText, sourceApp)
        if (geminiApiKey.isBlank()) return local

        return withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val prompt = """
你是台灣繁體中文記帳正規化解析器。今天日期是 $today。
來源 App：${sourceApp.ifBlank { "手動語音／文字輸入" }}。
規則：
1. 只需要輸出「一個」乾淨、可讀的繁體中文描述 description（絕不可照抄口語贅詞如「我今天、去找、買了、之類的、塊錢」，也絕不可截斷完整店名如「摩斯漢堡」）。
   範例：「我今天去找阿美自助餐買了早餐，180塊錢，吃了漢堡蛋餅奶茶之類的」 -> description: "阿美自助餐，漢堡、蛋餅、奶茶"
   範例：「您卡號末四碼 **** 於 摩斯漢堡 刷卡消費 NT$ 135」 -> description: "摩斯漢堡"
2. 回傳純 JSON（無 markdown 標記）：
{"type":"expense"|"income","description":"單一正規化描述","amount":數字,"category":"餐飲|交通|日常|購物|訂閱|帳單|娛樂|醫療|其他|薪資|獎金|投資|回饋|其他收入","date":"YYYY-MM-DD","paymentMethod":"現金|信用卡|交通卡|行動支付|銀行轉帳","isForeignTx":true|false}
使用者／通知輸入：$rawText
                """.trimIndent()

                val reqBody = """
                {
                  "contents": [{"parts": [{"text": ${json.encodeToString(String.serializer(), prompt)}}]}],
                  "generationConfig": {"maxOutputTokens": 200, "temperature": 0.1, "responseMimeType": "application/json"}
                }
                """.trimIndent()

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiApiKey")
                    .post(reqBody.toRequestBody(jsonMedia))
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext local
                    val bodyStr = resp.body?.string() ?: return@withContext local
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val candidateText = root["candidates"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
                        ?: return@withContext local

                    val cleanedJson = candidateText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val parsed = json.decodeFromString<ParsedEntry>(cleanedJson)
                    parsed.copy(
                        description = toTraditional(parsed.description.ifBlank { local.description }),
                        amount = if (parsed.amount > 0) parsed.amount else local.amount,
                        date = if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(parsed.date)) parsed.date else local.date,
                    )
                }
            } catch (_: Exception) {
                local
            }
        }
    }

    /**
     * 強化結構化本地解析：
     * 1. 先以銀行推播文法（於【商戶】刷卡/消費/付款）或完整商戶名單抽出完整店名（不再被子詞截斷！）
     * 2. 再將店名以外的子句抽出具體品項（漢堡、蛋餅、奶茶），合併為乾淨單一 description
     */
    fun parseLocally(rawText: String, sourceApp: String = ""): ParsedEntry {
        val text = toTraditional(rawText.trim())
        val isIncome = Regex("(收入|薪水|薪資|獎金|入帳|收到|回饋|退款|股息|發薪)").containsMatchIn(text)
        val type = if (isIncome) "income" else "expense"
        val isForeign = Regex("(海外|外幣|USD|JPY|EUR|KRW|日幣|美金|跨境)", RegexOption.IGNORE_CASE).containsMatchIn(text)

        val today = LocalDate.now()
        val date = when {
            text.contains("昨天") || text.contains("昨日") -> today.minusDays(1).toString()
            text.contains("前天") || text.contains("前日") -> today.minusDays(2).toString()
            else -> {
                val dm = Regex("""(\d{1,2})\s*[月/\-]\s*(\d{1,2})\s*[日號]?""").find(text)
                if (dm != null) {
                    val m = dm.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: today.monthValue
                    val d = dm.groupValues[2].toIntOrNull()?.coerceIn(1, 28) ?: today.dayOfMonth
                    LocalDate.of(today.year, m, d).toString()
                } else today.toString()
            }
        }

        val paymentMethod = when {
            Regex("(刷卡|信用卡|wallet|cube|richart|玉山|國泰|台新|中信|富邦)", RegexOption.IGNORE_CASE)
                .containsMatchIn("$sourceApp $text") -> "信用卡"
            Regex("(line\\s*pay|街口|行動支付|apple\\s*pay|samsung\\s*pay|悠遊付)", RegexOption.IGNORE_CASE)
                .containsMatchIn("$sourceApp $text") -> "行動支付"
            Regex("(悠遊卡|一卡通|交通卡|捷運卡)").containsMatchIn(text) -> "交通卡"
            Regex("(轉帳|匯款|網銀|入帳)").containsMatchIn(text) -> "銀行轉帳"
            else -> "現金"
        }

        // Amount extraction (ignoring masked **** and dates)
        val withoutDateAndMask = text
            .replace(Regex("""\d{1,2}\s*[月/\-]\s*\d{1,2}\s*[日號]?"""), " ")
            .replace(Regex("""\*+"""), " ")
        val amtMatch = Regex("""(?:NT\$|TWD|\$|金額|消費|刷卡|付款成功|交易|花了|付了|收到)?\s*([\d,]+(?:\.\d+)?)\s*(?:元|塊錢|塊|台幣)?""", RegexOption.IGNORE_CASE)
            .findAll(withoutDateAndMask)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .firstOrNull { it > 0.0 && it < 10_000_000.0 } ?: 0.0

        // Step 1: Extract intact merchant name first (bank notification pattern -> spoken pattern -> known brands)
        val bankMerchantRegex = Regex("""(?:於|在)\s*([^\s，,。、]{2,18}?)\s*(?:刷卡|消費|交易|付款|扣款)""")
        val spokenMerchantRegex = Regex("""(?:去找|去|到|在|找)\s*([^，,。、\s買吃喝點搭坐]{2,12}?)(?=(?:買|吃|喝|點|消費|付|搭|坐|，|,|。|\s|$))""")

        val merchant = bankMerchantRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: knownMerchants.firstOrNull { text.contains(it, ignoreCase = true) }
            ?: spokenMerchantRegex.find(text)?.groupValues?.get(1)?.removeSuffix("的")?.trim()
            ?: ""

        // Step 2: Scan remaining text (AFTER removing merchant string so "摩斯漢堡" won't trigger "漢堡"!)
        val remainderForItems = if (merchant.isNotEmpty()) text.replace(merchant, " ", ignoreCase = true) else text
        val items = mutableListOf<String>()
        var scanBuf = remainderForItems
        for (item in knownFoodItems) {
            if (scanBuf.contains(item)) {
                items.add(item)
                scanBuf = scanBuf.replace(item, " ")
            }
        }

        val description = when {
            merchant.isNotEmpty() && items.isNotEmpty() -> "$merchant，${items.joinToString("、")}"
            merchant.isNotEmpty() -> merchant
            items.isNotEmpty() -> items.joinToString("、")
            else -> {
                val cleaned = withoutDateAndMask
                    .replace(Regex("""(?:NT\$|TWD|\$)?\s*[\d,]+(?:\.\d+)?\s*(?:元|塊錢|塊|台幣)?""", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("""(我今天|今天|昨天|前天|剛剛|您卡號末四碼|末四碼|尾號|刷卡消費|刷卡交易|刷卡|信用卡|付款成功|消費|通知|然後|之類的|什麼的|買了|吃了|喝了|花了|付了|收到)"""), " ")
                    .replace(Regex("""[，,。、:：()（）【】\[\]]+"""), " ")
                    .trim()
                    .replace(Regex("""\s+"""), " ")
                cleaned.take(18).ifBlank { if (isIncome) "日常收入" else "日常消費" }
            }
        }

        val category = inferCategory("$merchant $description $text", isIncome)

        return ParsedEntry(
            type = type,
            description = description,
            amount = amtMatch,
            category = category,
            date = date,
            paymentMethod = paymentMethod,
            isForeignTx = isForeign,
        )
    }

    fun inferCategory(text: String, isIncome: Boolean): String {
        val lower = text.lowercase()
        if (isIncome) {
            return when {
                lower.contains("薪") -> "薪資"
                lower.contains("獎金") -> "獎金"
                lower.contains("回饋") || lower.contains("退款") -> "回饋"
                lower.contains("股") || lower.contains("投資") -> "投資"
                else -> "其他收入"
            }
        }
        return when {
            Regex("(便當|餐|飯|麵|拉麵|咖啡|星巴克|starbucks|麥當勞|摩斯|漢堡|蛋餅|奶茶|茶|飲料|火鍋|燒肉|ubereats|foodpanda|7-eleven|全家|早餐|午餐|晚餐|清心)").containsMatchIn(lower) -> "餐飲"
            Regex("(捷運|高鐵|台鐵|計程車|中油|加油|停車|uber|通勤|車票|機票)").containsMatchIn(lower) -> "交通"
            Regex("(全聯|家樂福|好市多|costco|屈臣氏|康是美|超市|日用|衛生紙|牛奶)").containsMatchIn(lower) -> "日常"
            Regex("(uniqlo|優衣庫|蝦皮|momo|百貨|服飾|apple|球鞋)").containsMatchIn(lower) -> "購物"
            Regex("(netflix|spotify|youtube|icloud|chatgpt|openai|會員|訂閱|月費)").containsMatchIn(lower) -> "訂閱"
            Regex("(房租|電費|水費|電信|寬頻|瓦斯|帳單|保險|管理費)").containsMatchIn(lower) -> "帳單"
            Regex("(電影|威秀|遊戲|steam|ktv|旅遊|門票)").containsMatchIn(lower) -> "娛樂"
            Regex("(醫院|診所|掛號|藥局|看牙|醫療)").containsMatchIn(lower) -> "醫療"
            else -> "其他"
        }
    }

    suspend fun generateMonthlyInsight(
        month: String,
        currency: String,
        income: Double,
        expense: Double,
        topCategory: String,
        topCategoryPct: Int,
        totalRewardPoints: Double,
        geminiApiKey: String,
    ): String {
        val monthNum = month.substringAfter("-").toIntOrNull() ?: 1
        val balance = income - expense
        val fallback = buildString {
            if (income == 0.0 && expense == 0.0) {
                append("${monthNum} 月尚無收支紀錄，點選右下角按鈕即可開始記帳。")
            } else {
                if (balance >= 0) {
                    append("${monthNum} 月結餘 $currency ${"%,.0f".format(balance)}")
                } else {
                    append("${monthNum} 月超支 $currency ${"%,.0f".format(-balance)}")
                }
                if (topCategory.isNotEmpty() && expense > 0) {
                    append("，主要開銷為${topCategory}（佔 ${topCategoryPct}%）")
                }
                if (totalRewardPoints > 0) {
                    append("，本月預估累積回饋 ${"%.0f".format(totalRewardPoints)} 點")
                }
                append("。")
            }
        }

        if (geminiApiKey.isBlank()) return fallback

        return withContext(Dispatchers.IO) {
            try {
                val prompt = "請根據 ${month} 個人收支數據，用「一句台灣繁體中文（42字內）」總結結餘與消費重點，口氣務實簡潔、禁用中二詞彙、不加引號。" +
                    "數據：貨幣=$currency，收入=$income，支出=$expense，結餘=$balance，最大支出=$topCategory($topCategoryPct%)，信用卡點數回饋=$totalRewardPoints。"
                val reqBody = """{"contents":[{"parts":[{"text":${json.encodeToString(String.serializer(), prompt)}}]}],"generationConfig":{"maxOutputTokens":90,"temperature":0.25}}"""
                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiApiKey")
                    .post(reqBody.toRequestBody(jsonMedia))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext fallback
                    val root = json.parseToJsonElement(resp.body?.string() ?: return@withContext fallback).jsonObject
                    val txt = root["candidates"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
                    txt?.trim('"', '「', '」')?.takeIf { it.isNotBlank() } ?: fallback
                }
            } catch (_: Exception) {
                fallback
            }
        }
    }
}
