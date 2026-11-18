package com.oneledger.samsung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.ParsedEntry
import com.oneledger.samsung.ui.components.ConfirmDeleteTransactionDialog
import com.oneledger.samsung.ui.components.LlmQuickSheet
import com.oneledger.samsung.ui.components.RadialGestureFab
import com.oneledger.samsung.ui.components.UnifiedEntrySheet
import com.oneledger.samsung.ui.screens.LedgerScreen
import com.oneledger.samsung.ui.screens.PlanScreen
import com.oneledger.samsung.ui.screens.SettingsFlowScreen
import com.oneledger.samsung.ui.screens.StatsScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val pendingDuplicates by viewModel.pendingDuplicates.collectAsState()
    val notificationLogs by viewModel.notificationLogs.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val insightText by viewModel.insightText.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val quickLaunchMode by viewModel.pendingQuickLaunch.collectAsState()

    var activeTab by remember { mutableStateOf("ledger") }
    var showUnifiedSheet by remember { mutableStateOf(false) }
    var unifiedStartPhoto by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var prefillEntry by remember { mutableStateOf<ParsedEntry?>(null) }
    var llmSheetMode by remember { mutableStateOf<String?>(null) } // "voice" | "text" | null
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun openCaptureMode(mode: String) {
        when (mode) {
            "voice", "text" -> llmSheetMode = mode
            "photo" -> {
                editingTx = null
                prefillEntry = null
                unifiedStartPhoto = true
                showUnifiedSheet = true
            }
            else -> {
                editingTx = null
                prefillEntry = null
                unifiedStartPhoto = false
                showUnifiedSheet = true
            }
        }
    }

    LaunchedEffect(quickLaunchMode) {
        quickLaunchMode?.let { mode ->
            openCaptureMode(mode)
            viewModel.consumeQuickLaunch()
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp),
            )
        },
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            when (activeTab) {
                "ledger" -> LedgerScreen(
                    selectedMonth = selectedMonth,
                    transactions = transactions,
                    pendingDuplicates = pendingDuplicates,
                    currency = currency,
                    insightText = insightText,
                    onShiftMonth = viewModel::shiftMonth,
                    onRefreshInsight = viewModel::refreshInsight,
                    onEditTransaction = { tx ->
                        editingTx = tx
                        prefillEntry = null
                        unifiedStartPhoto = false
                        showUnifiedSheet = true
                    },
                    onRequestDeleteTransaction = { pendingDeleteTx = it },
                    onApproveDuplicate = viewModel::approvePendingDuplicate,
                    onDismissDuplicate = viewModel::dismissPendingDuplicate,
                )

                "stats" -> StatsScreen(
                    selectedMonth = selectedMonth,
                    transactions = transactions,
                    creditCards = creditCards,
                    currency = currency,
                    onShiftMonth = viewModel::shiftMonth,
                    onEditTransaction = { tx ->
                        editingTx = tx
                        prefillEntry = null
                        unifiedStartPhoto = false
                        showUnifiedSheet = true
                    },
                    onRequestDeleteTransaction = { pendingDeleteTx = it },
                )

                "plan" -> PlanScreen(
                    debts = debts,
                    recurringRules = recurringRules,
                    creditCards = creditCards,
                    currency = currency,
                    onAddDebt = viewModel::addDebt,
                    onToggleDebtSettled = viewModel::toggleDebtSettled,
                    onDeleteDebt = viewModel::deleteDebt,
                    onAddRecurring = viewModel::addRecurringRule,
                    onToggleRecurring = viewModel::toggleRecurringRule,
                    onTriggerRecurring = viewModel::triggerRecurringNow,
                    onDeleteRecurring = viewModel::deleteRecurringRule,
                )

                "settings" -> SettingsFlowScreen(
                    prefs = viewModel.prefs,
                    themeMode = themeMode,
                    currency = currency,
                    creditCards = creditCards,
                    notificationLogs = notificationLogs,
                    transactions = transactions,
                    onChangeTheme = viewModel::setThemeMode,
                    onChangeCurrency = viewModel::setCurrency,
                    onSaveCard = viewModel::saveCreditCard,
                    onDeleteCard = viewModel::deleteCreditCard,
                    onSimulateNotification = viewModel::simulateNotification,
                    onSyncCloudNow = viewModel::syncCloudNow,
                    onUpdateReminder = viewModel::updateReminderSchedule,
                    onTestReminder = viewModel::sendTestReminderNotification,
                    onShowToast = viewModel::showToast,
                )
            }

            // Floating Samsung One UI Bottom Dock + Radial Gesture Action Button
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 10.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                    ) {
                        DockItem("帳本", Icons.Rounded.AccountBalanceWallet, activeTab == "ledger", Modifier.weight(1f)) { activeTab = "ledger" }
                        DockItem("統計", Icons.Rounded.BarChart, activeTab == "stats", Modifier.weight(1f)) { activeTab = "stats" }
                        DockItem("借還循環", Icons.Rounded.EventRepeat, activeTab == "plan", Modifier.weight(1f)) { activeTab = "plan" }
                        DockItem("設定", Icons.Rounded.Settings, activeTab == "settings", Modifier.weight(1f)) { activeTab = "settings" }
                    }
                }

                RadialGestureFab(onSelectMode = ::openCaptureMode)
            }
        }
    }

    if (showUnifiedSheet) {
        UnifiedEntrySheet(
            startWithPhoto = unifiedStartPhoto,
            editingItem = editingTx,
            prefill = prefillEntry,
            currency = currency,
            creditCards = creditCards,
            onDismiss = {
                showUnifiedSheet = false
                editingTx = null
                prefillEntry = null
            },
            onSave = { id, type, desc, amt, cat, pm, date, photo, cardId, isForeign ->
                viewModel.saveTransaction(id, type, desc, amt, cat, pm, date, photo, cardId, isForeign)
            },
            onRequestDelete = { pendingDeleteTx = it },
        )
    }

    llmSheetMode?.let { mode ->
        LlmQuickSheet(
            mode = mode,
            currency = currency,
            onParse = viewModel::parseNaturalSentence,
            onConfirmSave = { parsed ->
                viewModel.saveTransaction(
                    type = parsed.type,
                    description = parsed.description,
                    amount = parsed.amount,
                    category = parsed.category,
                    paymentMethod = parsed.paymentMethod,
                    date = parsed.date,
                    isForeignTx = parsed.isForeignTx,
                )
            },
            onOpenInEditor = { parsed ->
                editingTx = null
                prefillEntry = parsed
                unifiedStartPhoto = false
                showUnifiedSheet = true
            },
            onDismiss = { llmSheetMode = null },
        )
    }

    ConfirmDeleteTransactionDialog(
        target = pendingDeleteTx,
        currency = currency,
        onDismiss = { pendingDeleteTx = null },
        onConfirmDelete = viewModel::deleteTransaction,
    )
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
