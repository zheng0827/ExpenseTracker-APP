package com.oneledger.samsung.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.data.ledger.PendingDuplicateEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.ui.components.DonutBalanceCard
import com.oneledger.samsung.ui.components.MonthSwitcherPill
import com.oneledger.samsung.ui.components.SwipeableTransactionRow
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

fun formatDayTitle(dateStr: String): String {
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    val parts = dateStr.split("-")
    val short = if (parts.size == 3) "${parts[1].toIntOrNull() ?: 1}月${parts[2].toIntOrNull() ?: 1}日" else dateStr
    return when (dateStr) {
        today -> "今天 · $short"
        yesterday -> "昨天 · $short"
        else -> short
    }
}

@Composable
fun LedgerScreen(
    selectedMonth: YearMonth,
    transactions: List<TransactionEntity>,
    pendingDuplicates: List<PendingDuplicateEntity>,
    currency: String,
    insightText: String,
    onShiftMonth: (Long) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onRefreshInsight: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onRequestDeleteTransaction: (TransactionEntity) -> Unit,
    onApproveDuplicate: (PendingDuplicateEntity) -> Unit,
    onDismissDuplicate: (String) -> Unit,
    onNavigateToStats: ((String) -> Unit)? = null,
) {
    var swipedId by remember { mutableStateOf<String?>(null) }
    val monthPrefix = selectedMonth.toString()

    val monthTxs = remember(transactions, monthPrefix) {
        transactions.filter { it.date.startsWith(monthPrefix) }
    }
    val income = remember(monthTxs) { monthTxs.filter { it.type == "income" }.sumOf { it.amount } }
    val expense = remember(monthTxs) { monthTxs.filter { it.type != "income" }.sumOf { it.amount } }
    val grouped = remember(monthTxs) {
        monthTxs.groupBy { it.date }.entries.sortedByDescending { it.key }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("帳本", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                MonthSwitcherPill(
                    month = selectedMonth,
                    onPrev = { onShiftMonth(-1) },
                    onNext = { onShiftMonth(1) },
                    onSelectMonth = onSelectMonth,
                )
            }
        }

        // First-line LLM Balance Summary
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = insightText,
                        fontSize = 13.5.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRefreshInsight, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "更新摘要",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Donut Income / Expense / Balance Card
        item {
            DonutBalanceCard(
                income = income,
                expense = expense,
                currency = currency,
                onNavigateToStats = onNavigateToStats,
            )
        }

        // 待確認疑似重複通知（避免誤殺同金額的兩筆獨立消費！）
        if (pendingDuplicates.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "疑似重複通知待確認（${pendingDuplicates.size} 筆）",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        pendingDuplicates.forEach { dup ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    "${dup.sourceApp} · ${dup.description}（$currency ${"%,.0f".format(dup.amount)}）",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                )
                                Text(
                                    "與今日「${dup.matchedExistingTitle}」同金額，請確認是否為獨立消費：",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onDismissDuplicate(dup.id) }) {
                                        Text("是重複，略過", fontSize = 12.sp)
                                    }
                                    Button(onClick = { onApproveDuplicate(dup) }) {
                                        Text("不同筆，入帳", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (grouped.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("這個月份目前沒有收支紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            grouped.forEach { (dateStr, items) ->
                item(key = "day_$dateStr") {
                    val dayNet = items.sumOf { if (it.type == "income") it.amount else -it.amount }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                formatDayTitle(dateStr),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${if (dayNet >= 0) "+" else "-"}$currency ${"%,.0f".format(abs(dayNet))}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column {
                                items.forEachIndexed { idx, tx ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    SwipeableTransactionRow(
                                        item = tx,
                                        currency = currency,
                                        isOpen = swipedId == tx.id,
                                        onSwipeStateChange = { swipedId = it },
                                        onEdit = onEditTransaction,
                                        onRequestDelete = onRequestDeleteTransaction,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
