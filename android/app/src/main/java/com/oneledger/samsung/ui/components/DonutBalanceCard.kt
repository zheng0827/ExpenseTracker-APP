package com.oneledger.samsung.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import kotlin.math.abs

/**
 * 首頁與統計頁共用之「甜甜圈收支結餘圓餅圖」
 */
@Composable
fun DonutBalanceCard(
    income: Double,
    expense: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val balance = income - expense
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val expensePct = when {
        income > 0 -> ((expense / income) * 100).toInt().coerceAtMost(999)
        expense > 0 -> 100
        else -> 0
    }
    val balancePct = if (income > 0) ((balance / income) * 100).toInt().coerceAtLeast(0) else 0

    val balanceSweep = if (income > 0 && balance > 0) ((balance / income).toFloat().coerceIn(0f, 1f)) * 360f else 0f
    val expenseSweep = when {
        income > 0 && expense <= income -> ((expense / income).toFloat().coerceIn(0f, 1f)) * 360f
        expense > 0 -> 360f
        else -> 0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(124.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(116.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Butt)
                    drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
                    if (balanceSweep > 0f) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = balanceSweep,
                            useCenter = false,
                            style = stroke,
                        )
                    }
                    if (expenseSweep > 0f) {
                        drawArc(
                            color = ExpenseCoralDark,
                            startAngle = -90f + balanceSweep,
                            sweepAngle = expenseSweep,
                            useCenter = false,
                            style = stroke,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (balance >= 0) "本月結餘" else "本月超支",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${if (balance < 0) "-" else ""}${"%,.0f".format(abs(balance))}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance < 0) ExpenseCoralDark else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (income > 0) "結餘率 $balancePct%" else currency,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LegendRow("收入", IncomeGreenDark, null, "+$currency ${"%,.0f".format(income)}", IncomeGreenDark)
                LegendRow("支出", ExpenseCoralDark, "$expensePct%", "-$currency ${"%,.0f".format(expense)}", ExpenseCoralDark)
                LegendRow(
                    "結餘",
                    accentColor,
                    if (balance >= 0) "$balancePct%" else "超支",
                    "${if (balance < 0) "-" else ""}$currency ${"%,.0f".format(abs(balance))}",
                    if (balance < 0) ExpenseCoralDark else accentColor,
                )
            }
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    dotColor: Color,
    pctText: String?,
    amountText: String,
    amountColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pctText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pctText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
        Text(
            text = amountText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor,
        )
    }
}
