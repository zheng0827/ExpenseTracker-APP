package com.oneledger.samsung.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.data.SecurePrefs
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.NotificationLogEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsPage {
    HOME,
    NOTIFICATION_LISTENER,
    CREDIT_CARDS,
    CLOUD_SYNC,
    GEMINI_AI,
    REMINDER,
    APPEARANCE_EXPORT,
}

/**
 * 三星 One UI 經典「設定主清單 → 二級子頁」結構，告別過長單頁捲動。
 */
@Composable
fun SettingsFlowScreen(
    prefs: SecurePrefs,
    themeMode: String,
    currency: String,
    creditCards: List<CreditCardEntity>,
    notificationLogs: List<NotificationLogEntity>,
    transactions: List<TransactionEntity>,
    onChangeTheme: (String) -> Unit,
    onChangeCurrency: (String) -> Unit,
    onSaveCard: (String, String, Double, Double, String, Double) -> Unit,
    onDeleteCard: (String) -> Unit,
    onSimulateNotification: (String, String, String) -> Unit,
    onSyncCloudNow: () -> Unit,
    onUpdateReminder: (Boolean, Int, Int) -> Unit,
    onTestReminder: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    var currentPage by remember { mutableStateOf(SettingsPage.HOME) }

    BackHandler(enabled = currentPage != SettingsPage.HOME) {
        currentPage = SettingsPage.HOME
    }

    when (currentPage) {
        SettingsPage.HOME -> SettingsHomeList(
            prefs = prefs,
            themeMode = themeMode,
            cardCount = creditCards.size,
            onNavigate = { currentPage = it },
        )

        SettingsPage.NOTIFICATION_LISTENER -> NotificationListenerSubScreen(
            prefs = prefs,
            logs = notificationLogs,
            currency = currency,
            onBack = { currentPage = SettingsPage.HOME },
            onSimulate = onSimulateNotification,
            onShowToast = onShowToast,
        )

        SettingsPage.CREDIT_CARDS -> CreditCardsSubScreen(
            cards = creditCards,
            onBack = { currentPage = SettingsPage.HOME },
            onAddCard = onSaveCard,
            onDeleteCard = onDeleteCard,
        )

        SettingsPage.CLOUD_SYNC -> CloudSyncSubScreen(
            prefs = prefs,
            onBack = { currentPage = SettingsPage.HOME },
            onSyncNow = onSyncCloudNow,
            onShowToast = onShowToast,
        )

        SettingsPage.GEMINI_AI -> GeminiSubScreen(
            prefs = prefs,
            onBack = { currentPage = SettingsPage.HOME },
            onShowToast = onShowToast,
        )

        SettingsPage.REMINDER -> ReminderSubScreen(
            prefs = prefs,
            onBack = { currentPage = SettingsPage.HOME },
            onUpdateReminder = onUpdateReminder,
            onTestReminder = onTestReminder,
        )

        SettingsPage.APPEARANCE_EXPORT -> AppearanceExportSubScreen(
            prefs = prefs,
            themeMode = themeMode,
            currency = currency,
            transactions = transactions,
            onBack = { currentPage = SettingsPage.HOME },
            onChangeTheme = onChangeTheme,
            onChangeCurrency = onChangeCurrency,
            onShowToast = onShowToast,
        )
    }
}

@Composable
private fun SettingsHomeList(
    prefs: SecurePrefs,
    themeMode: String,
    cardCount: Int,
    onNavigate: (SettingsPage) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("設定", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column {
                    SettingsMenuRow(
                        icon = Icons.Rounded.NotificationsActive,
                        iconBg = Color(0xFF0381FE),
                        title = "通知監聽器與卡號去敏",
                        subtitle = if (prefs.listenerEnabled) "即時監聽中 · 卡號後四碼本地遮蔽" else "已暫停",
                        onClick = { onNavigate(SettingsPage.NOTIFICATION_LISTENER) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.CreditCard,
                        iconBg = Color(0xFF8B5CF6),
                        title = "信用卡點數回饋設定",
                        subtitle = "已設定 $cardCount 張回饋規則（支援海外 1.5% 淨回饋）",
                        onClick = { onNavigate(SettingsPage.CREDIT_CARDS) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.CloudSync,
                        iconBg = Color(0xFF148A4B),
                        title = "多裝置雲端 SQLite 同步",
                        subtitle = if (prefs.syncServerUrl.isNotBlank()) "已設定伺服器" else "僅本機離線 SQLite",
                        onClick = { onNavigate(SettingsPage.CLOUD_SYNC) },
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column {
                    SettingsMenuRow(
                        icon = Icons.Rounded.AutoAwesome,
                        iconBg = Color(0xFFEC4899),
                        title = "AI 語意解析設定（Gemini）",
                        subtitle = if (prefs.geminiApiKey.isNotBlank()) "已加密儲存金鑰" else "使用內建本地正規化解析",
                        onClick = { onNavigate(SettingsPage.GEMINI_AI) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.Schedule,
                        iconBg = Color(0xFFF59E0B),
                        title = "推播通知與每日提醒",
                        subtitle = if (prefs.dailyReminderEnabled) "每日 %02d:%02d 提醒記帳".format(prefs.dailyReminderHour, prefs.dailyReminderMinute) else "已關閉",
                        onClick = { onNavigate(SettingsPage.REMINDER) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.Palette,
                        iconBg = Color(0xFF06B6D4),
                        title = "外觀主題、貨幣與匯出",
                        subtitle = when (themeMode) {
                            "dark" -> "深色 AMOLED 純黑"
                            "light" -> "淺色日間"
                            else -> "跟隨系統"
                        } + " · ${prefs.currency}",
                        onClick = { onNavigate(SettingsPage.APPEARANCE_EXPORT) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回設定")
        }
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotificationListenerSubScreen(
    prefs: SecurePrefs,
    logs: List<NotificationLogEntity>,
    currency: String,
    onBack: () -> Unit,
    onSimulate: (String, String, String) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.listenerEnabled) }
    var maskLast4 by remember { mutableStateOf(prefs.maskCardLast4) }
    var appsCsv by remember { mutableStateOf(prefs.monitoredAppsCsv) }
    var keywords by remember { mutableStateOf(prefs.listenerKeywords) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SubScreenTopBar("通知監聽器與卡號去敏", onBack) }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                    ) {
                        Text("前往三星系統設定開啟「通知存取權」")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("即時監聽通知自動記帳", fontWeight = FontWeight.SemiBold)
                            Text("攔截指定 App 推播後立即送 LLM 分析", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enabled, onCheckedChange = {
                            enabled = it
                            prefs.listenerEnabled = it
                        })
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(IncomeGreenDark.copy(alpha = 0.13f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = IncomeGreenDark)
                            Column {
                                Text("本地端遮蔽信用卡末四碼", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text("送交 LLM 前強制馬賽克卡號與驗證碼", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = maskLast4, onCheckedChange = {
                            maskLast4 = it
                            prefs.maskCardLast4 = it
                        })
                    }

                    OutlinedTextField(
                        value = appsCsv,
                        onValueChange = { appsCsv = it },
                        label = { Text("監聽 App 名稱（逗號分隔）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("觸發關鍵字（逗號分隔）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            prefs.monitoredAppsCsv = appsCsv
                            prefs.listenerKeywords = keywords
                            onShowToast("已儲存監聽規則")
                        },
                    ) {
                        Text("儲存監聽設定")
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            onSimulate(
                                "玉山 Wallet",
                                "刷卡消費通知",
                                "您卡號末四碼 4821 於 摩斯漢堡 刷卡消費 NT$ 135",
                            )
                        },
                    ) {
                        Text("實測模擬通知（驗證遮蔽 4821 與摩斯漢堡解析）")
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("最近去敏與解析紀錄", fontWeight = FontWeight.Bold)
                        logs.take(6).forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    "${log.appName} · ${log.reason}${if (log.amount > 0) " ($currency ${"%,.0f".format(log.amount)})" else ""}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                )
                                if (log.redactedText.isNotBlank()) {
                                    Text("去敏內容：${log.redactedText}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardsSubScreen(
    cards: List<CreditCardEntity>,
    onBack: () -> Unit,
    onAddCard: (String, String, Double, Double, String, Double) -> Unit,
    onDeleteCard: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("點數") }
    var baseRate by remember { mutableStateOf("1.0") }
    var bonusRate by remember { mutableStateOf("3.0") }
    var bonusCats by remember { mutableStateOf("餐飲,日常,購物") }
    var foreignRate by remember { mutableStateOf("3.0") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SubScreenTopBar("信用卡點數回饋設定", onBack) }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("新增信用卡回饋規則（不需卡號，不追蹤帳戶餘額）", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("卡片名稱") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("點數單位") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = baseRate, onValueChange = { baseRate = it }, label = { Text("一般 %") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = bonusRate, onValueChange = { bonusRate = it }, label = { Text("加碼 %") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = foreignRate, onValueChange = { foreignRate = it }, label = { Text("海外 %") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = bonusCats, onValueChange = { bonusCats = it }, label = { Text("加碼分類（逗號分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = name.isNotBlank(),
                        onClick = {
                            onAddCard(
                                name,
                                unit,
                                baseRate.toDoubleOrNull() ?: 1.0,
                                bonusRate.toDoubleOrNull() ?: 3.0,
                                bonusCats,
                                foreignRate.toDoubleOrNull() ?: 3.0,
                            )
                            name = ""
                        },
                    ) {
                        Text("新增卡片規則")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column {
                    cards.forEachIndexed { idx, card ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${card.name}（${card.rewardUnit}）", fontWeight = FontWeight.Bold)
                                Text(
                                    "一般 ${card.baseRatePercent}% · 加碼 ${card.bonusRatePercent}%（${card.bonusCategoriesCsv}）· 海外 ${card.foreignRatePercent}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDeleteCard(card.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "刪除", tint = ExpenseCoralDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncSubScreen(
    prefs: SecurePrefs,
    onBack: () -> Unit,
    onSyncNow: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    var serverUrl by remember { mutableStateOf(prefs.syncServerUrl) }
    var token by remember { mutableStateOf(prefs.syncToken) }
    val lastSyncFormatted = if (prefs.lastSyncAt > 0) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN).format(Date(prefs.lastSyncAt))
    } else "尚未同步"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SubScreenTopBar("多裝置雲端 SQLite 同步", onBack) }
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "平時所有記帳完全寫入本機 SQLite（離線秒開秒記），連線自架 sync-server 後自動由背景 WorkManager 雙向同步多裝置。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("裝置識別碼：${prefs.deviceId} · 上次同步：$lastSyncFormatted", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("同步伺服器網址（例如 https://sync.yourdomain.com）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Bearer 同步金鑰（AES256 加密保存）") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                prefs.syncServerUrl = serverUrl
                                prefs.syncToken = token
                                onShowToast("已加密儲存雲端同步設定")
                            },
                        ) { Text("儲存設定") }
                        Button(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                prefs.syncServerUrl = serverUrl
                                prefs.syncToken = token
                                onSyncNow()
                            },
                        ) { Text("立即同步") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiSubScreen(
    prefs: SecurePrefs,
    onBack: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SubScreenTopBar("AI 語意解析設定（Gemini）", onBack) }
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "填入 Gemini API Key 後，語音記帳、文字記帳、推播通知正規化與首頁結餘一句總結將直接由 Gemini 2.5 Flash 驅動；金鑰使用 Android Keystore 加密儲存。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gemini API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            prefs.geminiApiKey = apiKey
                            onShowToast(if (apiKey.isBlank()) "已清除金鑰，改用本地解析" else "已加密儲存 Gemini 金鑰")
                        },
                    ) {
                        Text("儲存金鑰")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSubScreen(
    prefs: SecurePrefs,
    onBack: () -> Unit,
    onUpdateReminder: (Boolean, Int, Int) -> Unit,
    onTestReminder: () -> Unit,
) {
    var autoPush by remember { mutableStateOf(prefs.pushOnAutoRecord) }
    var dailyEnabled by remember { mutableStateOf(prefs.dailyReminderEnabled) }
    var hourText by remember { mutableStateOf(prefs.dailyReminderHour.toString()) }
    var minText by remember { mutableStateOf("%02d".format(prefs.dailyReminderMinute)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SubScreenTopBar("推播與每日提醒", onBack) }
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自動記帳完成時推播", fontWeight = FontWeight.SemiBold)
                            Text("監聽通知或循環自動入帳時立即推播確認", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoPush, onCheckedChange = {
                            autoPush = it
                            prefs.pushOnAutoRecord = it
                        })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("每日提醒「今天記帳了嗎？」", fontWeight = FontWeight.SemiBold)
                            Text("透過 WorkManager 背景排程，無需開啟 App", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = dailyEnabled, onCheckedChange = {
                            dailyEnabled = it
                            onUpdateReminder(it, hourText.toIntOrNull() ?: 21, minText.toIntOrNull() ?: 30)
                        })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hourText, onValueChange = { hourText = it }, label = { Text("時 (0-23)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = minText, onValueChange = { minText = it }, label = { Text("分 (0-59)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { onUpdateReminder(dailyEnabled, hourText.toIntOrNull() ?: 21, minText.toIntOrNull() ?: 30) },
                        ) { Text("套用排程") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            onClick = onTestReminder,
                        ) { Text("測試推播") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceExportSubScreen(
    prefs: SecurePrefs,
    themeMode: String,
    currency: String,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onChangeTheme: (String) -> Unit,
    onChangeCurrency: (String) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val context = LocalContext.current
    var currentWidgetBg by remember { mutableStateOf(prefs.widgetBackgroundStyle) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SubScreenTopBar("外觀主題、貨幣與匯出", onBack) }
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("外觀主題", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "跟隨系統", "light" to "淺色", "dark" to "AMOLED 黑").forEach { (k, lbl) ->
                            val active = themeMode == k
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onChangeTheme(k) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(lbl, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("桌面小工具底色風格", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "obsidian" to "曜石深黑",
                            "amoled" to "極致純黑",
                            "glass" to "毛玻璃",
                            "light" to "簡約淺白",
                        ).forEach { (k, lbl) ->
                            val active = currentWidgetBg == k
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        currentWidgetBg = k
                                        prefs.widgetBackgroundStyle = k
                                        onShowToast("已套用小工具外觀：$lbl")
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    lbl,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("貨幣符號", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NT$", "¥", "$", "HK$").forEach { cur ->
                            val active = currency == cur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onChangeCurrency(cur) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(cur, fontWeight = FontWeight.Bold, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            val header = "日期,時間,類型,描述,分類,金額,支付方式,回饋點數"
                            val rows = transactions.joinToString("\n") { tx ->
                                val typeStr = if (tx.type == "income") "收入" else "支出"
                                val safeDesc = tx.description.replace(",", " ")
                                "${tx.date},${tx.time},$typeStr,$safeDesc,${tx.category},${tx.amount},${tx.paymentMethod},${tx.rewardPoints}"
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "簡記收支明細匯出")
                                putExtra(Intent.EXTRA_TEXT, "$header\n$rows")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "匯出 CSV 帳單"))
                        },
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("分享／匯出 CSV 帳單")
                    }
                }
            }
        }
    }
}
