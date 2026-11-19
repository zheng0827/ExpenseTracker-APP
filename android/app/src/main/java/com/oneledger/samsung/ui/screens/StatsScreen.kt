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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.Categories
import com.oneledger.samsung.ui.components.DonutBalanceCard
import com.oneledger.samsung.ui.components.MonthSwitcherPill
import com.oneledger.samsung.ui.components.MonthlyBalanceLineChartCard
import com.oneledger.samsung.ui.components.SpendingTrendChartCard
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import java.time.YearMonth

/**
 * 統計頁面：
 * 順序依序為：
 * 1. 標題與月份膠囊
 * 2. 雙色收支結餘甜甜圈（無藍色殘留，中間為結餘金額，收入與支出可點選切換檢視）
 * 3. 搜尋列與篩選晶片（全部／支出／收入／含照片）
 * 4. 支出／收入總額橫幅提示
 * 5. 唯讀明細列表（禁止左滑刪除與修改，點擊可開啟唯讀照片與詳細對話框）
 * 6. 消費走勢圖與 6 個月折線圖（支援觸控滑動即時查值）
 * 7. 分類佔比與照片牆
 */
@Composable
fun StatsScreen(
    selectedMonth: YearMonth,
    transactions: List<TransactionEntity>,
    creditCards: List<CreditCardEntity>,
    currency: String,
    initialFilter: String? = null,
    onShiftMonth: (Long) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(initialFilter ?: "all") }
    var viewingDetailTx by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(initialFilter) {
        if (!initialFilter.isNullOrBlank()) {
            filter = initialFilter
        }
    }

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

    val photoTxs = remember(monthTxs) { monthTxs.filter { !it.photoUri.isNullOrBlank() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. 標題與月份切換膠囊
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

        // 2. 收支結餘甜甜圈圖（頂部常駐，點擊右側收入/支出直接連動本頁篩選）
        item {
            DonutBalanceCard(
                income = income,
                expense = expense,
                currency = currency,
                onNavigateToStats = { clickedType ->
                    filter = clickedType
                },
            )
        }

        // 3. 搜尋欄位與分類篩選標籤
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
                        "expense" to "支出 (${monthTxs.count { it.type != "income" }})",
                        "income" to "收入 (${monthTxs.count { it.type == "income" }})",
                        "photo" to "含照片 (${photoTxs.size})",
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

        // 4. 支出或收入總額醒目橫幅
        if (filter == "expense" || filter == "income") {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (filter == "income") IncomeGreenDark.copy(alpha = 0.12f) else ExpenseCoralDark.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (filter == "income") "本月收入總額" else "本月支出總額",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (filter == "income") IncomeGreenDark else ExpenseCoralDark,
                        )
                        Text(
                            text = if (filter == "income") "+$currency ${"%,.0f".format(income)}" else "-$currency ${"%,.0f".format(expense)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (filter == "income") IncomeGreenDark else ExpenseCoralDark,
                        )
                    }
                }
            }
        }

        // 5. 唯讀明細列表（統計頁面內禁止編輯與刪除，純供查閱與檢視照片）
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("消費與收入明細", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("共 ${filteredTxs.size} 筆（唯讀查閱）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (filteredTxs.isEmpty()) {
                        Text(
                            "本月無符合條件之紀錄",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    } else {
                        filteredTxs.forEachIndexed { idx, tx ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ReadOnlyTransactionRow(
                                item = tx,
                                currency = currency,
                                onClick = { viewingDetailTx = tx },
                            )
                        }
                    }
                }
            }
        }

        // 6. 消費走勢圖（支援觸控滑動顯示單日精準數值）
        item {
            SpendingTrendChartCard(month = selectedMonth, transactions = transactions, currency = currency)
        }

        // 7. 近 6 個月收支折線圖（支援觸控滑動查值）
        item {
            MonthlyBalanceLineChartCard(selectedMonth = selectedMonth, transactions = transactions, currency = currency)
        }

        // 8. 支出分類佔比進度列
        if (byCategory.isNotEmpty()) {
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

        // 9. 照片牆（點擊即開啟全螢幕高解析檢視）
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
                                        .clickable { viewingDetailTx = tx },
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = tx.photoUri,
                                            contentDescription = tx.description,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f),
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

    // 唯讀詳情與照片檢視對話框
    viewingDetailTx?.let { tx ->
        ReadOnlyTransactionDetailDialog(
            tx = tx,
            currency = currency,
            onDismiss = { viewingDetailTx = null },
        )
    }
}

/**
 * 統計頁專用之唯讀消費列（無滑動手勢、不可直接編輯，點擊開啟照片與詳情）
 */
@Composable
private fun ReadOnlyTransactionRow(
    item: TransactionEntity,
    currency: String,
    onClick: () -> Unit,
) {
    val catInfo = Categories.find(item.category)
    val isIncome = item.type == "income"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catInfo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(catInfo.icon, contentDescription = null, tint = catInfo.color, modifier = Modifier.size(22.dp))
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.description.ifBlank { item.category },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!item.photoUri.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = "含照片",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = "${item.date} · ${item.paymentMethod}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "${if (isIncome) "+" else "-"}$currency ${"%,.0f".format(item.amount)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) IncomeGreenDark else ExpenseCoralDark,
        )
    }
}

/**
 * 統計頁面之唯讀詳情與大圖預覽彈窗
 */
@Composable
private fun ReadOnlyTransactionDetailDialog(
    tx: TransactionEntity,
    currency: String,
    onDismiss: () -> Unit,
) {
    val isIncome = tx.type == "income"
    val catInfo = Categories.find(tx.category)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(catInfo.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(catInfo.icon, contentDescription = null, tint = catInfo.color, modifier = Modifier.size(18.dp))
                        }
                        Text(tx.category, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "關閉")
                    }
                }

                // 金額大字
                Text(
                    text = "${if (isIncome) "+" else "-"}$currency ${"%,.0f".format(tx.amount)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) IncomeGreenDark else ExpenseCoralDark,
                )

                // 描述
                if (tx.description.isNotBlank()) {
                    Text(
                        text = tx.description,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // 屬性資訊
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("日期", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.date, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("支付方式", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.paymentMethod, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    if (tx.source.isNotBlank() && tx.source != "manual") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("記錄來源", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                when (tx.source) {
                                    "notification" -> "銀行通知自動記帳"
                                    "recurring" -> "每月固定循環記帳"
                                    "voice" -> "語音智慧辨識"
                                    else -> tx.source
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // 消費照片檢視
                if (!tx.photoUri.isNullOrBlank()) {
                    Text("消費發票／單據照片", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    AsyncImage(
                        model = tx.photoUri,
                        contentDescription = "消費單據照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("確定")
                }
            }
        }
    }
}
