package com.oneledger.samsung.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.data.debt.DebtEntity
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.RecurringRuleEntity
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import java.time.YearMonth

/**
 * 借還款（獨立 SQLite debts.db，純備忘紀錄不入主帳本）與 循環記帳（WorkManager 自動到期入帳）
 */
@Composable
fun PlanScreen(
    debts: List<DebtEntity>,
    recurringRules: List<RecurringRuleEntity>,
    creditCards: List<CreditCardEntity>,
    currency: String,
    onAddDebt: (direction: String, person: String, amount: Double, reason: String, dueDate: String) -> Unit,
    onToggleDebtSettled: (DebtEntity) -> Unit,
    onDeleteDebt: (String) -> Unit,
    onAddRecurring: (description: String, type: String, amount: Double, category: String, paymentMethod: String, dayOfMonth: Int, cardId: String?) -> Unit,
    onToggleRecurring: (RecurringRuleEntity) -> Unit,
    onTriggerRecurring: (RecurringRuleEntity) -> Unit,
    onDeleteRecurring: (String) -> Unit,
) {
    var subTab by remember { mutableStateOf("debts") } // "debts" | "recurring"
    var showAddForm by remember { mutableStateOf(false) }

    // Debt Form
    var debtDir by remember { mutableStateOf("lent") }
    var debtPerson by remember { mutableStateOf("") }
    var debtAmt by remember { mutableStateOf("") }
    var debtReason by remember { mutableStateOf("") }

    // Recurring Form
    var recTitle by remember { mutableStateOf("") }
    var recType by remember { mutableStateOf("expense") }
    var recAmt by remember { mutableStateOf("") }
    var recDay by remember { mutableStateOf("15") }

    val openDebts = remember(debts) { debts.filter { !it.settled } }
    val lentTotal = remember(openDebts) { openDebts.filter { it.direction == "lent" }.sumOf { it.amount } }
    val borrowedTotal = remember(openDebts) { openDebts.filter { it.direction == "borrowed" }.sumOf { it.amount } }
    val currentMonth = YearMonth.now().toString()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(if (subTab == "debts") "借還款" else "循環記帳", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .padding(4.dp),
            ) {
                listOf("debts" to "借還款（獨立庫）", "recurring" to "循環記帳").forEach { (key, label) ->
                    val active = subTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                subTab = key
                                showAddForm = false
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (subTab == "debts") {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill("別人欠我", "$currency ${"%,.0f".format(lentTotal)}", IncomeGreenDark, Modifier.weight(1f))
                            SummaryPill("我欠別人", "$currency ${"%,.0f".format(borrowedTotal)}", ExpenseCoralDark, Modifier.weight(1f))
                        }
                        Text(
                            "存放在獨立 debts.db 資料庫，結清僅標記備忘，絕不影響收支圓餅圖與月統計。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { showAddForm = !showAddForm },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(if (showAddForm) "收合表單" else "新增借還款紀錄")
                        }
                    }
                }
            }

            if (showAddForm) {
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                    .padding(3.dp),
                            ) {
                                listOf("lent" to "我借出（別人欠我）", "borrowed" to "我借入（我欠別人）").forEach { (k, lbl) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (debtDir == k) MaterialTheme.colorScheme.surface else Color.Transparent)
                                            .clickable { debtDir = k }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(lbl, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = debtPerson,
                                    onValueChange = { debtPerson = it },
                                    label = { Text("對象") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = debtAmt,
                                    onValueChange = { debtAmt = it },
                                    label = { Text("金額") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            OutlinedTextField(
                                value = debtReason,
                                onValueChange = { debtReason = it },
                                label = { Text("事由（例如：聚餐代墊）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                enabled = debtPerson.isNotBlank() && (debtAmt.toDoubleOrNull() ?: 0.0) > 0,
                                onClick = {
                                    onAddDebt(debtDir, debtPerson, debtAmt.toDouble(), debtReason, "")
                                    debtPerson = ""
                                    debtAmt = ""
                                    debtReason = ""
                                    showAddForm = false
                                },
                            ) {
                                Text("儲存至借還款庫")
                            }
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
                        if (debts.isEmpty()) {
                            Text("目前沒有借還款紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
                        } else {
                            debts.forEachIndexed { idx, d ->
                                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${d.person} · ${if (d.direction == "lent") "別人欠我" else "我欠別人"}${if (d.settled) "（已結清）" else ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = if (d.settled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            "${d.reason.ifBlank { "未填事由" }} · ${d.date}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "$currency ${"%,.0f".format(d.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (d.settled) MaterialTheme.colorScheme.onSurfaceVariant
                                            else if (d.direction == "lent") IncomeGreenDark else ExpenseCoralDark,
                                        )
                                        IconButton(onClick = { onToggleDebtSettled(d) }) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = "標記結清",
                                                tint = if (d.settled) IncomeGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { onDeleteDebt(d.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "刪除", tint = ExpenseCoralDark, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 循環記帳分頁
            item {
                val monthlyFixed = recurringRules.filter { it.active && it.type != "income" }.sumOf { it.amount }
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill("每月固定支出", "$currency ${"%,.0f".format(monthlyFixed)}", ExpenseCoralDark, Modifier.weight(1f))
                            SummaryPill("啟用規則數", "${recurringRules.count { it.active }} 筆", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        }
                        Text(
                            "背景 WorkManager 會於每月扣款日自動寫入帳本（同月冪等防重複）並推送通知。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { showAddForm = !showAddForm },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(if (showAddForm) "收合表單" else "新增循環訂閱／固定收支")
                        }
                    }
                }
            }

            if (showAddForm) {
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = recTitle,
                                onValueChange = { recTitle = it },
                                label = { Text("項目名稱（例如：Spotify、YouTube Premium）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = recAmt,
                                    onValueChange = { recAmt = it },
                                    label = { Text("金額") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = recDay,
                                    onValueChange = { recDay = it },
                                    label = { Text("每月幾號(1-28)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { recType = if (recType == "expense") "income" else "expense" },
                                ) {
                                    Text(if (recType == "expense") "類型：固定支出" else "類型：固定收入")
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    enabled = recTitle.isNotBlank() && (recAmt.toDoubleOrNull() ?: 0.0) > 0,
                                    onClick = {
                                        onAddRecurring(
                                            recTitle,
                                            recType,
                                            recAmt.toDouble(),
                                            if (recType == "income") "薪資" else "訂閱",
                                            if (recType == "income") "銀行轉帳" else "信用卡",
                                            recDay.toIntOrNull() ?: 15,
                                            if (recType == "expense") creditCards.firstOrNull()?.id else null,
                                        )
                                        recTitle = ""
                                        recAmt = ""
                                        showAddForm = false
                                    },
                                ) {
                                    Text("建立規則")
                                }
                            }
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
                        recurringRules.forEachIndexed { idx, r ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${r.description}${if (r.lastPostedMonth == currentMonth) " · 本月已入帳" else ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                        )
                                        Text(
                                            "每月 ${r.dayOfMonth} 日 · ${r.category} · ${r.paymentMethod}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(checked = r.active, onCheckedChange = { onToggleRecurring(r) })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "${if (r.type == "income") "+" else "-"}$currency ${"%,.0f".format(r.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (r.type == "income") IncomeGreenDark else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = { onTriggerRecurring(r) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Text("立即入帳", fontSize = 12.sp)
                                        }
                                        IconButton(onClick = { onDeleteRecurring(r.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "刪除", tint = ExpenseCoralDark, modifier = Modifier.size(18.dp))
                                        }
                                    }
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
private fun SummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = color)
        Text(label, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
