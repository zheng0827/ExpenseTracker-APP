package com.oneledger.samsung.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneledger.samsung.OneLedgerApp
import com.oneledger.samsung.data.debt.DebtEntity
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.PendingDuplicateEntity
import com.oneledger.samsung.data.ledger.RecurringRuleEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.ParsedEntry
import com.oneledger.samsung.domain.SmartEntryParser
import com.oneledger.samsung.service.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OneLedgerApp
    val repo = app.repository
    val prefs = app.prefs

    val transactions = repo.transactionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = repo.debtsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurringRules = repo.recurringFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val creditCards = repo.creditCardsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingDuplicates = repo.pendingDuplicatesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notificationLogs = repo.notificationLogsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _currency = MutableStateFlow(prefs.currency)
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _insightText = MutableStateFlow("正在整理本月收支結餘...")
    val insightText: StateFlow<String> = _insightText.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Deep-link launch from Jetpack Glance Home Screen Widgets ("manual" | "voice" | "text" | "photo")
    private val _pendingQuickLaunch = MutableStateFlow<String?>(null)
    val pendingQuickLaunch: StateFlow<String?> = _pendingQuickLaunch.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            repo.postDueRecurringRules()
        }
        viewModelScope.launch {
            combine(transactions, selectedMonth, currency) { txs, ym, cur -> Triple(txs, ym, cur) }
                .collect { (txs, ym, cur) ->
                    refreshInsightInternal(txs, ym, cur)
                }
        }
    }

    fun triggerQuickLaunch(mode: String) {
        _pendingQuickLaunch.value = mode
    }

    fun consumeQuickLaunch() {
        _pendingQuickLaunch.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun shiftMonth(delta: Long) {
        _selectedMonth.value = _selectedMonth.value.plusMonths(delta)
    }

    fun setYearMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun refreshInsight() {
        viewModelScope.launch {
            refreshInsightInternal(transactions.value, selectedMonth.value, currency.value)
        }
    }

    private suspend fun refreshInsightInternal(txs: List<TransactionEntity>, ym: YearMonth, cur: String) {
        val monthStr = ym.toString()
        val monthTxs = txs.filter { it.date.startsWith(monthStr) }
        val income = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
        val expense = monthTxs.filter { it.type != "income" }.sumOf { it.amount }
        val topCatEntry = monthTxs.filter { it.type != "income" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }
        val topCat = topCatEntry?.key ?: ""
        val topPct = if (expense > 0 && topCatEntry != null) ((topCatEntry.value / expense) * 100).toInt() else 0
        val totalRewards = monthTxs.sumOf { it.rewardPoints }

        _insightText.value = SmartEntryParser.generateMonthlyInsight(
            month = monthStr,
            currency = cur,
            income = income,
            expense = expense,
            topCategory = topCat,
            topCategoryPct = topPct,
            totalRewardPoints = totalRewards,
            geminiApiKey = prefs.geminiApiKey,
        )
    }

    fun saveTransaction(
        id: String? = null,
        type: String,
        description: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        date: String,
        photoUri: String? = null,
        creditCardId: String? = null,
        isForeignTx: Boolean = false,
    ) {
        viewModelScope.launch {
            val saved = repo.saveTransaction(
                id = id,
                type = type,
                description = description,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                date = date,
                photoUri = photoUri,
                creditCardId = creditCardId,
                isForeignTx = isForeignTx,
            )
            val sign = if (saved.type == "income") "+" else "-"
            val rewardSuffix = if (saved.rewardPoints > 0) "（+${"%.1f".format(saved.rewardPoints)} ${saved.rewardUnit}）" else ""
            showToast("已儲存：${saved.description} $sign${currency.value} ${"%,.0f".format(saved.amount)}$rewardSuffix")
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repo.deleteTransaction(id)
            showToast("已刪除該筆紀錄")
        }
    }

    suspend fun parseNaturalSentence(sentence: String): ParsedEntry {
        return SmartEntryParser.parse(sentence, prefs.geminiApiKey)
    }

    // 借還款（獨立 debts.db，絕不影響收支統計）
    fun addDebt(direction: String, person: String, amount: Double, reason: String, dueDate: String) {
        viewModelScope.launch {
            repo.saveDebt(direction, person, amount, reason, LocalDate.now().toString(), dueDate)
            showToast("已記錄借還款：$person ${currency.value} ${"%,.0f".format(amount)}")
        }
    }

    fun toggleDebtSettled(debt: DebtEntity) {
        viewModelScope.launch {
            repo.toggleDebtSettled(debt)
            showToast(if (debt.settled) "已恢復未結清：${debt.person}" else "已標記結清（不計入收支）：${debt.person}")
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch {
            repo.deleteDebt(id)
            showToast("已刪除借還紀錄")
        }
    }

    // 循環記帳
    fun addRecurringRule(
        description: String,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        dayOfMonth: Int,
        creditCardId: String?,
    ) {
        viewModelScope.launch {
            repo.saveRecurringRule(
                RecurringRuleEntity(
                    id = "rec_${UUID.randomUUID()}",
                    description = description.trim(),
                    type = type,
                    amount = amount,
                    category = category,
                    paymentMethod = paymentMethod,
                    creditCardId = creditCardId,
                    dayOfMonth = dayOfMonth.coerceIn(1, 28),
                    active = true,
                )
            )
            showToast("已建立循環規則：$description")
        }
    }

    fun toggleRecurringRule(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repo.saveRecurringRule(rule.copy(active = !rule.active))
        }
    }

    fun triggerRecurringNow(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            val entry = repo.triggerRecurringRule(rule)
            showToast("已入帳：${entry.description} ${currency.value} ${"%,.0f".format(entry.amount)}")
        }
    }

    fun deleteRecurringRule(id: String) {
        viewModelScope.launch {
            repo.deleteRecurringRule(id)
            showToast("已刪除循環規則")
        }
    }

    // 信用卡點數回饋設定
    fun saveCreditCard(
        name: String,
        rewardUnit: String,
        baseRate: Double,
        bonusRate: Double,
        bonusCategories: String,
        foreignRate: Double,
    ) {
        viewModelScope.launch {
            repo.saveCreditCard(
                CreditCardEntity(
                    id = "card_${UUID.randomUUID()}",
                    name = name.trim(),
                    rewardUnit = rewardUnit.trim().ifBlank { "點數" },
                    baseRatePercent = baseRate,
                    bonusRatePercent = bonusRate,
                    bonusCategoriesCsv = bonusCategories.trim(),
                    foreignRatePercent = foreignRate,
                    foreignFeePercent = 1.5,
                )
            )
            showToast("已儲存信用卡回饋規則：$name")
        }
    }

    fun deleteCreditCard(id: String) {
        viewModelScope.launch {
            repo.deleteCreditCard(id)
            showToast("已移除該張信用卡規則")
        }
    }

    // 待確認重複通知
    fun approvePendingDuplicate(item: PendingDuplicateEntity) {
        viewModelScope.launch {
            repo.approvePendingDuplicate(item)
            showToast("已確認入帳：${item.description}")
        }
    }

    fun dismissPendingDuplicate(id: String) {
        viewModelScope.launch {
            repo.dismissPendingDuplicate(id)
            showToast("已略過重複通知")
        }
    }

    // 測試通知監聽（即時驗證本地馬賽克後四碼 → LLM → 入帳 → 系統推播）
    fun simulateNotification(appName: String, title: String, body: String) {
        viewModelScope.launch {
            val log = repo.handleIncomingNotification(appName, title, body)
            showToast(log.reason)
        }
    }

    fun sendTestReminderNotification() {
        NotificationHelper.sendDailyReminder(
            context = app,
            title = "今天記帳了嗎？",
            body = "花 5 秒鐘記錄今天的收支吧。",
        )
        showToast("已發送測試系統通知")
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            val outcome = repo.syncWithCloudServer()
            showToast(outcome.message)
        }
    }

    fun setThemeMode(mode: String) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setCurrency(cur: String) {
        prefs.currency = cur
        _currency.value = cur
        viewModelScope.launch { repo.refreshWidgets() }
    }

    fun updateReminderSchedule(enabled: Boolean, hour: Int, minute: Int) {
        prefs.dailyReminderEnabled = enabled
        prefs.dailyReminderHour = hour
        prefs.dailyReminderMinute = minute
        app.rescheduleDailyReminder()
        showToast(if (enabled) "每日提醒已設為 %02d:%02d".format(hour, minute) else "已關閉每日提醒")
    }
}
