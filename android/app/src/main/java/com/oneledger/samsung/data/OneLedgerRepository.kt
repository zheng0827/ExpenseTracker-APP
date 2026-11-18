package com.oneledger.samsung.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.oneledger.samsung.data.debt.DebtDatabase
import com.oneledger.samsung.data.debt.DebtEntity
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.LedgerDatabase
import com.oneledger.samsung.data.ledger.NotificationLogEntity
import com.oneledger.samsung.data.ledger.PendingDuplicateEntity
import com.oneledger.samsung.data.ledger.RecurringRuleEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.CardRedactor
import com.oneledger.samsung.domain.RewardCalculator
import com.oneledger.samsung.domain.SmartEntryParser
import com.oneledger.samsung.service.NotificationHelper
import com.oneledger.samsung.widget.BalanceQuickWidget
import com.oneledger.samsung.widget.QuickAddBarWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Serializable
private data class SyncChangesDto(
    val transactions: List<TransactionEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val recurring: List<RecurringRuleEntity> = emptyList(),
    val credit_cards: List<CreditCardEntity> = emptyList(),
)

@Serializable
private data class SyncRequestDto(
    val deviceId: String,
    val since: Long,
    val changes: SyncChangesDto,
)

@Serializable
private data class SyncResponseDto(
    val ok: Boolean = false,
    val serverTime: Long = 0L,
    val appliedCount: Int = 0,
    val pull: SyncChangesDto = SyncChangesDto(),
)

data class SyncOutcome(
    val ok: Boolean,
    val message: String,
)

class OneLedgerRepository(
    private val context: Context,
    private val ledgerDb: LedgerDatabase,
    private val debtDb: DebtDatabase,
    val prefs: SecurePrefs,
) {
    private val txDao = ledgerDb.transactionDao()
    private val recDao = ledgerDb.recurringDao()
    private val cardDao = ledgerDb.creditCardDao()
    private val dupDao = ledgerDb.pendingDuplicateDao()
    private val notifLogDao = ledgerDb.notificationLogDao()
    private val debtDao = debtDb.debtDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    val transactionsFlow: Flow<List<TransactionEntity>> = txDao.observeAllActive()
    val recurringFlow: Flow<List<RecurringRuleEntity>> = recDao.observeAllActive()
    val creditCardsFlow: Flow<List<CreditCardEntity>> = cardDao.observeAllActive()
    val pendingDuplicatesFlow: Flow<List<PendingDuplicateEntity>> = dupDao.observeAll()
    val notificationLogsFlow: Flow<List<NotificationLogEntity>> = notifLogDao.observeRecent()
    // 借還款完全來自獨立的 debts.db
    val debtsFlow: Flow<List<DebtEntity>> = debtDao.observeAllActive()

    suspend fun ensureSeeded() {
        if (prefs.seeded) return
        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val curMonth = ym.toString()

        val cards = listOf(
            CreditCardEntity("card_cube", "國泰 CUBE", "小樹點", 0.3, 3.0, "餐飲,日常,交通", 3.0, 1.5),
            CreditCardEntity("card_ubear", "玉山 U Bear", "現金折抵", 1.0, 3.0, "訂閱,購物,娛樂", 1.0, 1.5),
            CreditCardEntity("card_gogo", "台新 @GoGo", "台新 Point", 0.5, 3.8, "餐飲,購物,日常", 2.0, 1.5),
        )
        cardDao.upsertAll(cards)

        val seedTxs = listOf(
            TransactionEntity(
                id = "tx_seed_1", type = "expense", description = "招牌排骨便當",
                amount = 120.0, category = "餐飲", paymentMethod = "現金",
                date = today.toString(), time = "12:18",
            ),
            TransactionEntity(
                id = "tx_seed_2", type = "expense", description = "星巴克，大杯冰拿鐵",
                amount = 145.0, category = "餐飲", paymentMethod = "信用卡",
                date = today.toString(), time = "09:12",
                creditCardId = "card_cube", rewardPoints = 4.4, rewardUnit = "小樹點",
            ),
            TransactionEntity(
                id = "tx_seed_3", type = "expense", description = "全聯，鮮乳、雞蛋",
                amount = 248.0, category = "日常", paymentMethod = "信用卡",
                date = today.minusDays(1).toString(), time = "19:40",
                creditCardId = "card_cube", rewardPoints = 7.4, rewardUnit = "小樹點",
            ),
            TransactionEntity(
                id = "tx_seed_4", type = "expense", description = "房租與管理費",
                amount = 14500.0, category = "帳單", paymentMethod = "銀行轉帳",
                date = ym.atDay(5).toString(), time = "10:00",
            ),
            TransactionEntity(
                id = "tx_seed_5", type = "income", description = "本月薪資入帳",
                amount = 52000.0, category = "薪資", paymentMethod = "銀行轉帳",
                date = ym.atDay(5).toString(), time = "09:00",
            ),
            TransactionEntity(
                id = "tx_seed_6", type = "expense", description = "Spotify Premium",
                amount = 149.0, category = "訂閱", paymentMethod = "信用卡",
                date = ym.atDay(5).toString(), time = "08:00", source = "recurring",
                creditCardId = "card_ubear", rewardPoints = 4.5, rewardUnit = "現金折抵",
            ),
            // 上月資料（供切換月份與 6 個月折線圖呈現）
            TransactionEntity(
                id = "tx_prev_1", type = "income", description = "薪資入帳",
                amount = 52000.0, category = "薪資", paymentMethod = "銀行轉帳",
                date = ym.minusMonths(1).atDay(5).toString(), time = "09:00",
            ),
            TransactionEntity(
                id = "tx_prev_2", type = "expense", description = "房租與日常開銷",
                amount = 23800.0, category = "帳單", paymentMethod = "銀行轉帳",
                date = ym.minusMonths(1).atDay(12).toString(), time = "12:00",
            ),
            TransactionEntity(
                id = "tx_prev_3", type = "income", description = "薪資與獎金",
                amount = 58000.0, category = "薪資", paymentMethod = "銀行轉帳",
                date = ym.minusMonths(2).atDay(5).toString(), time = "09:00",
            ),
            TransactionEntity(
                id = "tx_prev_4", type = "expense", description = "生活費與旅行",
                amount = 28500.0, category = "娛樂", paymentMethod = "信用卡",
                date = ym.minusMonths(2).atDay(18).toString(), time = "15:00",
                creditCardId = "card_cube", rewardPoints = 85.5, rewardUnit = "小樹點",
            ),
        )
        txDao.upsertAll(seedTxs)

        val rules = listOf(
            RecurringRuleEntity("rec_spotify", "Spotify Premium", "expense", 149.0, "訂閱", "信用卡", "card_ubear", "monthly", 5, true, curMonth),
            RecurringRuleEntity("rec_yt", "YouTube Premium", "expense", 199.0, "訂閱", "信用卡", "card_ubear", "monthly", 12, true, ""),
            RecurringRuleEntity("rec_gpt", "ChatGPT Plus", "expense", 650.0, "訂閱", "信用卡", "card_ubear", "monthly", 25, true, ""),
            RecurringRuleEntity("rec_salary", "每月薪資入帳", "income", 52000.0, "薪資", "銀行轉帳", null, "monthly", 5, true, curMonth),
        )
        recDao.upsertAll(rules)

        // 獨立借還款資料庫（debts.db）
        val seedDebts = listOf(
            DebtEntity("debt_seed_1", "lent", "阿明", 650.0, 0.0, "週五聚餐代墊", today.minusDays(1).toString(), today.plusDays(5).toString(), false),
            DebtEntity("debt_seed_2", "borrowed", "小傑", 320.0, 0.0, "電影票代刷", today.minusDays(2).toString(), today.plusDays(3).toString(), false),
        )
        debtDao.upsertAll(seedDebts)

        prefs.seeded = true
        refreshWidgets()
    }

    suspend fun saveTransaction(
        id: String? = null,
        type: String,
        description: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        date: String,
        time: String? = null,
        photoUri: String? = null,
        source: String = "manual",
        sourceApp: String? = null,
        creditCardId: String? = null,
        isForeignTx: Boolean = false,
    ): TransactionEntity {
        val card = if (paymentMethod == "信用卡" && !creditCardId.isNullOrBlank()) {
            cardDao.getById(creditCardId)
        } else null

        val reward = if (type == "expense" && card != null) {
            RewardCalculator.calculate(amount, category, isForeignTx, card)
        } else null

        val nowTime = time ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val entity = TransactionEntity(
            id = id ?: "tx_${UUID.randomUUID()}",
            type = type,
            description = description.trim().ifBlank { "${category}${if (type == "income") "收入" else "消費"}" },
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            date = date,
            time = nowTime,
            photoUri = photoUri,
            source = source,
            sourceApp = sourceApp,
            creditCardId = card?.id,
            isForeignTx = isForeignTx,
            rewardPoints = reward?.grossPoints ?: 0.0,
            rewardUnit = reward?.rewardUnit ?: "",
            updatedAt = System.currentTimeMillis(),
            syncPending = true,
        )
        txDao.upsert(entity)
        refreshWidgets()
        return entity
    }

    suspend fun deleteTransaction(id: String) {
        txDao.softDelete(id)
        refreshWidgets()
    }

    // ==================== 借還款（完全隔離在 debts.db，絕不影響 ledger.db） ====================
    suspend fun saveDebt(
        direction: String,
        person: String,
        amount: Double,
        reason: String,
        date: String,
        dueDate: String,
    ) {
        debtDao.upsert(
            DebtEntity(
                id = "debt_${UUID.randomUUID()}",
                direction = direction,
                person = person.trim(),
                amount = amount,
                reason = reason.trim(),
                date = date,
                dueDate = dueDate,
                settled = false,
                updatedAt = System.currentTimeMillis(),
                syncPending = true,
            )
        )
    }

    suspend fun toggleDebtSettled(debt: DebtEntity) {
        val nextSettled = !debt.settled
        debtDao.upsert(
            debt.copy(
                settled = nextSettled,
                paidAmount = if (nextSettled) debt.amount else 0.0,
                updatedAt = System.currentTimeMillis(),
                syncPending = true,
            )
        )
    }

    suspend fun deleteDebt(id: String) {
        debtDao.softDelete(id)
    }

    // ==================== 循環記帳（含 WorkManager 背景自動到期入帳） ====================
    suspend fun saveRecurringRule(rule: RecurringRuleEntity) {
        recDao.upsert(rule.copy(updatedAt = System.currentTimeMillis(), syncPending = true))
    }

    suspend fun deleteRecurringRule(id: String) {
        recDao.softDelete(id)
    }

    suspend fun triggerRecurringRule(rule: RecurringRuleEntity): TransactionEntity {
        val today = LocalDate.now()
        val entry = saveTransaction(
            type = rule.type,
            description = rule.description,
            amount = rule.amount,
            category = rule.category,
            paymentMethod = rule.paymentMethod,
            date = today.toString(),
            source = "recurring",
            creditCardId = rule.creditCardId,
        )
        recDao.upsert(
            rule.copy(
                lastPostedMonth = YearMonth.from(today).toString(),
                updatedAt = System.currentTimeMillis(),
                syncPending = true,
            )
        )
        return entry
    }

    suspend fun postDueRecurringRules(): Int {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today).toString()
        val activeRules = recDao.getActiveRules()
        var postedCount = 0

        for (rule in activeRules) {
            if (rule.lastPostedMonth == currentMonth) continue
            if (today.dayOfMonth >= rule.dayOfMonth) {
                val entry = triggerRecurringRule(rule)
                postedCount++
                if (prefs.pushOnAutoRecord) {
                    NotificationHelper.sendAutoRecordNotification(
                        context = context,
                        title = "循環記帳已自動入帳",
                        body = "${entry.description} ${if (entry.type == "income") "+" else "-"}${prefs.currency} ${"%,.0f".format(entry.amount)}",
                    )
                }
            }
        }
        return postedCount
    }

    // ==================== 信用卡回饋設定 ====================
    suspend fun saveCreditCard(card: CreditCardEntity) {
        cardDao.upsert(card.copy(updatedAt = System.currentTimeMillis(), syncPending = true))
    }

    suspend fun deleteCreditCard(id: String) {
        cardDao.softDelete(id)
    }

    // ==================== 通知監聽處理（本地遮蔽後四碼 → LLM → 安全去重／待確認佇列） ====================
    suspend fun handleIncomingNotification(appName: String, title: String, body: String): NotificationLogEntity {
        if (!prefs.listenerEnabled) {
            return logNotification(appName, "ignored", "通知監聽器未啟用", "", 0, 0.0)
        }

        val monitored = prefs.getMonitoredAppList()
        val appHit = monitored.isEmpty() || monitored.any {
            appName.contains(it, ignoreCase = true) || it.contains(appName, ignoreCase = true)
        }
        if (!appHit) {
            return logNotification(appName, "ignored", "不在監聽清單內", "", 0, 0.0)
        }

        val rawCombined = listOf(title, body).filter { it.isNotBlank() }.joinToString(" ").trim()
        val keywords = prefs.listenerKeywords.split(",", "，", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        val kwHit = keywords.isEmpty() || keywords.any { rawCombined.contains(it, ignoreCase = true) }
        if (!kwHit) {
            return logNotification(appName, "ignored", "未命中關鍵字", "", 0, 0.0)
        }

        // 本地先馬掉信用卡後四碼與敏感號碼，絕不裸送外部 LLM
        val redaction = if (prefs.maskCardLast4) {
            CardRedactor.redact(rawCombined)
        } else {
            com.oneledger.samsung.domain.RedactionResult(rawCombined, 0)
        }

        val parsed = SmartEntryParser.parse(redaction.redactedText, prefs.geminiApiKey, appName)
        if (parsed.amount <= 0.0) {
            return logNotification(appName, "ignored", "未解析出有效金額", redaction.redactedText, redaction.maskedCount, 0.0)
        }

        // 安全去重：先查當日同金額紀錄
        val sameDayTxs = txDao.getByDate(parsed.date)
        val sameAmtTx = sameDayTxs.firstOrNull {
            it.type == parsed.type && abs(it.amount - parsed.amount) < 0.01
        }

        if (sameAmtTx != null) {
            val similarity = computeSimpleSimilarity(sameAmtTx.description, parsed.description)
            if (similarity >= 0.6) {
                // 商戶高度一致 -> 安全略過不重複記帳
                return logNotification(
                    appName, "deduplicated",
                    "與當日「${sameAmtTx.description}」吻合，已略過重複",
                    redaction.redactedText, redaction.maskedCount, parsed.amount,
                )
            } else {
                // 同金額但商戶不確定（例如一天買兩杯 65 元飲料）-> 放進「待確認重複」不靜默吃掉！
                dupDao.insert(
                    PendingDuplicateEntity(
                        id = "dup_${UUID.randomUUID()}",
                        sourceApp = appName,
                        description = parsed.description,
                        amount = parsed.amount,
                        category = parsed.category,
                        date = parsed.date,
                        time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                        redactedText = redaction.redactedText,
                        matchedExistingTitle = sameAmtTx.description,
                    )
                )
                if (prefs.pushOnAutoRecord) {
                    NotificationHelper.sendAutoRecordNotification(
                        context = context,
                        title = "偵測到疑似重複消費，待你確認",
                        body = "$appName：${parsed.description} ${prefs.currency} ${"%,.0f".format(parsed.amount)}（與今日「${sameAmtTx.description}」同金額）",
                    )
                }
                return logNotification(
                    appName, "pending_duplicate",
                    "與當日「${sameAmtTx.description}」同金額但描述不同，已移入待確認",
                    redaction.redactedText, redaction.maskedCount, parsed.amount,
                )
            }
        }

        // 嘗試對應使用者設定的信用卡回饋規則
        val allCards = cardDao.getAllActive()
        val matchedCard = allCards.firstOrNull { card ->
            val bankPrefix = card.name.take(2)
            appName.contains(bankPrefix, ignoreCase = true) || redaction.redactedText.contains(bankPrefix, ignoreCase = true)
        }

        val created = saveTransaction(
            type = parsed.type,
            description = parsed.description,
            amount = parsed.amount,
            category = parsed.category,
            paymentMethod = parsed.paymentMethod,
            date = parsed.date,
            source = "notification",
            sourceApp = appName,
            creditCardId = matchedCard?.id,
            isForeignTx = parsed.isForeignTx,
        )

        val maskNote = if (redaction.maskedCount > 0) "（已遮蔽卡號）" else ""
        if (prefs.pushOnAutoRecord) {
            NotificationHelper.sendAutoRecordNotification(
                context = context,
                title = "已自動記帳 · $appName",
                body = "${created.description} -${prefs.currency} ${"%,.0f".format(created.amount)}$maskNote",
            )
        }

        return logNotification(
            appName, "recorded",
            "已自動分析入帳$maskNote",
            redaction.redactedText, redaction.maskedCount, created.amount,
        )
    }

    suspend fun approvePendingDuplicate(item: PendingDuplicateEntity) {
        saveTransaction(
            type = "expense",
            description = item.description,
            amount = item.amount,
            category = item.category,
            paymentMethod = "信用卡",
            date = item.date,
            time = item.time,
            source = "notification",
            sourceApp = item.sourceApp,
        )
        dupDao.deleteById(item.id)
    }

    suspend fun dismissPendingDuplicate(id: String) {
        dupDao.deleteById(id)
    }

    private suspend fun logNotification(
        appName: String,
        status: String,
        reason: String,
        redactedText: String,
        maskedCount: Int,
        amount: Double,
    ): NotificationLogEntity {
        val log = NotificationLogEntity(
            id = "nl_${UUID.randomUUID()}",
            appName = appName,
            status = status,
            reason = reason,
            redactedText = redactedText,
            maskedCount = maskedCount,
            amount = amount,
        )
        notifLogDao.insert(log)
        return log
    }

    private fun computeSimpleSimilarity(a: String, b: String): Double {
        val sa = a.lowercase().replace(Regex("""[\s，,、。]"""), "")
        val sb = b.lowercase().replace(Regex("""[\s，,、。]"""), "")
        if (sa.isEmpty() || sb.isEmpty()) return 0.0
        if (sa.contains(sb) || sb.contains(sa)) return 1.0
        val setA = sa.toSet()
        val setB = sb.toSet()
        val common = setA.intersect(setB).size
        return common.toDouble() / minOf(setA.size, setB.size).coerceAtLeast(1)
    }

    // ==================== 多裝置雲端 SQLite 增量同步 ====================
    suspend fun syncWithCloudServer(): SyncOutcome = withContext(Dispatchers.IO) {
        val serverUrl = prefs.syncServerUrl
        if (serverUrl.isBlank()) {
            return@withContext SyncOutcome(false, "尚未設定雲端同步伺服器網址")
        }

        try {
            val pendingTxs = txDao.getPendingSync()
            val pendingDebts = debtDao.getPendingSync()
            val pendingRec = recDao.getPendingSync()
            val pendingCards = cardDao.getPendingSync()

            val payload = SyncRequestDto(
                deviceId = prefs.deviceId,
                since = prefs.lastSyncAt,
                changes = SyncChangesDto(
                    transactions = pendingTxs,
                    debts = pendingDebts,
                    recurring = pendingRec,
                    credit_cards = pendingCards,
                ),
            )

            val request = Request.Builder()
                .url("$serverUrl/v1/sync")
                .header("Authorization", "Bearer ${prefs.syncToken}")
                .post(json.encodeToString(payload).toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext SyncOutcome(false, "雲端伺服器回應錯誤 (${resp.code})")
                }
                val respDto = json.decodeFromString<SyncResponseDto>(resp.body?.string() ?: "{}")

                if (pendingTxs.isNotEmpty()) txDao.markSynced(pendingTxs.map { it.id })
                if (pendingDebts.isNotEmpty()) debtDao.markSynced(pendingDebts.map { it.id })
                if (pendingRec.isNotEmpty()) recDao.markSynced(pendingRec.map { it.id })
                if (pendingCards.isNotEmpty()) cardDao.markSynced(pendingCards.map { it.id })

                if (respDto.pull.transactions.isNotEmpty()) {
                    txDao.upsertAll(respDto.pull.transactions.map { it.copy(syncPending = false) })
                }
                if (respDto.pull.debts.isNotEmpty()) {
                    debtDao.upsertAll(respDto.pull.debts.map { it.copy(syncPending = false) })
                }
                if (respDto.pull.recurring.isNotEmpty()) {
                    recDao.upsertAll(respDto.pull.recurring.map { it.copy(syncPending = false) })
                }
                if (respDto.pull.credit_cards.isNotEmpty()) {
                    cardDao.upsertAll(respDto.pull.credit_cards.map { it.copy(syncPending = false) })
                }

                prefs.lastSyncAt = respDto.serverTime
                refreshWidgets()
                SyncOutcome(
                    true,
                    "同步完成：上傳 ${respDto.appliedCount} 筆，下載 ${respDto.pull.transactions.size + respDto.pull.debts.size} 筆",
                )
            }
        } catch (e: Exception) {
            SyncOutcome(false, "同步失敗：${e.message ?: "無法連線"}")
        }
    }

    suspend fun getTodayTransactionCount(): Int {
        return txDao.getByDate(LocalDate.now().toString()).size
    }

    suspend fun refreshWidgets() {
        try {
            BalanceQuickWidget().updateAll(context)
            QuickAddBarWidget().updateAll(context)
        } catch (_: Exception) {
            // Widget not pinned yet
        }
    }
}
