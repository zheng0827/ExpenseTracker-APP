package com.oneledger.samsung.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.Categories
import com.oneledger.samsung.ui.components.DonutBalanceCard
import com.oneledger.samsung.ui.components.MonthSwitcherPill
import com.oneledger.samsung.ui.components.MonthlyBalanceLineChartCard
import com.oneledger.samsung.ui.components.SpendingTrendChartCard
import com.oneledger.samsung.ui.components.SwipeableTransactionRow
import java.io.File
import java.time.YearMonth

@Composable
fun StatsScreen(
    selectedMonth: YearMonth,
    transactions: List<TransactionEntity>,
    creditCards: List<CreditCardEntity>,
    currency: String,
    onShiftMonth: (Long) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onRequestDeleteTransaction: (TransactionEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var swipedId by remember { mutableStateOf<String?>(null) }

    val monthPrefix = selectedMonth.toString()
    val monthTxs = remember(transactions, monthPrefix) {
        transactions.filter { it.date.startsWith(monthPrefix) }
    }
    val income = remember(monthTxs) { monthTxs.filter { it.type == "income" }.sumOf { it.amount } }
    val expense = remember(monthTxs) { monthTxs.filter { it.type != "income" }.sumOf { it.amount } }

    val filteredTxs = remember(monthTxs, query, filter) {
        val q = query.trim().lowercase()
        monthTxs.filter { tx ->
            val passFilter = when (filter) {
                "expense" -> tx.type != "income"
                "income" -> tx.type == "income"
                "photo" -> !tx.photoUri.isNullOrBlank()
                "notification" -> tx.source == "notification"
                "recurring" -> tx.source == "recurring"
                else -> true
            }
            passFilter && (q.isEmpty() || tx.description.lowercase().contains(q) || tx.category.lowercase().contains(q))
        }
    }

    val byCategory = remember(monthTxs, expense) {
        monthTxs.filter { it.type != "income" }
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                Triple(cat, sum, if (expense > 0) ((sum / expense) * 100).toInt() else 0)
            }
            .sortedByDescending { it.second }
    }

    // 本月各張信用卡點數／現金回饋彙總（無帳戶餘額，純粹追蹤點數回饋與海外淨回饋）
    val cardRewards = remember(monthTxs, creditCards) {
        monthTxs.filter { it.rewardPoints > 0 }
            .groupBy { it.creditCardId ?: "other" }
            .map { (cardId, list) ->
                val cardName = creditCards.firstOrNull { it.id == cardId }?.name ?: "信用卡"
                val unit = list.firstOrNull()?.rewardUnit.orEmpty().ifBlank { "點" }
                val pts = list.sumOf { it.rewardPoints }
                val spend = list.sumOf { it.amount }
                Triple("$cardName（$unit）", pts, spend)
            }
            .sortedByDescending { it.second }
    }

    val photoTxs = remember(monthTxs) { monthTxs.filter { !it.photoUri.isNullOrBlank() } }

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
                Text("統計", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                MonthSwitcherPill(
                    month = selectedMonth,
                    onPrev = { onShiftMonth(-1) },
                    onNext = { onShiftMonth(1) },
                    onSelectMonth = onSelectMonth,
                )
            }
        }

        // Search + Filter Chips (Moved cleanly from Home)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("搜尋描述、分類...") },
                    singleLine = true,
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "all" to "全部 (${monthTxs.size})",
                        "expense" to "支出",
                        "income" to "收入",
                        "photo" to "含照片",
                        "notification" to "通知自動",
                        "recurring" to "循環",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick = { filter = key },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }
            }
        }

        if (query.isNotBlank() || filter != "all") {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        if (filteredTxs.isEmpty()) {
                            Text(
                                "找不到符合條件的紀錄",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp),
                            )
                        } else {
                            filteredTxs.forEachIndexed { idx, tx ->
                                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                SwipeableTransactionRow(
                                    item = tx,
                                    currency = currency,
                                    subtitlePrefix = formatDayTitle(tx.date),
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

        // Donut Chart
        item {
            DonutBalanceCard(income = income, expense = expense, currency = currency)
        }

        // Daily / Cumulative Spending Trend
        item {
            SpendingTrendChartCard(month = selectedMonth, transactions = transactions, currency = currency)
        }

        // 6-Month Income / Expense / Balance Line Chart
        item {
            MonthlyBalanceLineChartCard(selectedMonth = selectedMonth, transactions = transactions, currency = currency)
        }

        // Credit Card Rewards Summary Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("本月信用卡點數／回饋預估", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    if (cardRewards.isEmpty()) {
                        Text("本月尚無套用信用卡回饋規則的消費", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        cardRewards.forEach { (label, pts, spend) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "累積刷卡 $currency ${"%,.0f".format(spend)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "+${"%.1f".format(pts)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expense Category Breakdown
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("支出分類佔比", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (byCategory.isEmpty()) {
                        Text("本月尚無支出分類資料", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        byCategory.forEach { (cat, sum, pct) ->
                            val info = Categories.find(cat)
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(info.color.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(info.icon, contentDescription = null, tint = info.color, modifier = Modifier.size(15.dp))
                                        }
                                        Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    Text(
                                        "$currency ${"%,.0f".format(sum)} · $pct%",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0.03f, 1f) },
                                    color = info.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Photo Gallery Wall
        if (photoTxs.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "消費照片牆（${photoTxs.size} 張）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    photoTxs.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { tx ->
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditTransaction(tx) },
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = File(tx.photoUri!!),
                                            contentDescription = tx.description,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(4f / 3f),
                                        )
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(tx.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("$currency ${"%,.0f".format(tx.amount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
