package com.oneledger.samsung.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import kotlin.math.abs

/**
 * 首頁與統計頁共用之「甜甜圈收支結餘圓餅圖」
 * - 僅顯示 收入（綠色）與 支出（珊瑚紅）雙段（無藍色殘留）
 * - 正中間顯示「結餘」與具體金額數值（不顯示結餘率或百分比）
 * - 右側僅保留「收入」與「支出」兩組互動區塊，點擊可直接連結跳轉至統計頁面查看對應明細
 */
@Composable
fun DonutBalanceCard(
    income: Double,
    expense: Double,
    currency: String,
    modifier: Modifier = Modifier,
    onNavigateToStats: ((filterType: String) -> Unit)? = null,
) {
    val balance = income - expense
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val total = income + expense

    val incomeSweep = if (total > 0) ((income / total) * 360.0).toFloat() else 0f
    val expenseSweep = if (total > 0) ((expense / total) * 360.0).toFloat() else 0f

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
            // 甜甜圈圓餅圖
            Box(
                modifier = Modifier.size(126.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(116.dp)) {
                    val stroke = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Butt)
                    drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)

                    if (incomeSweep > 0f) {
                        drawArc(
                            color = IncomeGreenDark,
                            startAngle = -90f,
                            sweepAngle = incomeSweep,
                            useCenter = false,
                            style = stroke,
                        )
                    }
                    if (expenseSweep > 0f) {
                        drawArc(
                            color = ExpenseCoralDark,
                            startAngle = -90f + incomeSweep,
                            sweepAngle = expenseSweep,
                            useCenter = false,
                            style = stroke,
                        )
                    }
                }

                // 正中間：直接顯示結餘與數值（無百分比／結餘率）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = if (balance >= 0) "結餘" else "超支",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${if (balance < 0) "-" else ""}$currency ${"%,.0f".format(abs(balance))}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance < 0) ExpenseCoralDark else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // 右側：收入與支出 互動按鈕（可點擊直達統計頁面）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionLegendRow(
                    label = "收入",
                    dotColor = IncomeGreenDark,
                    amountText = "+$currency ${"%,.0f".format(income)}",
                    amountColor = IncomeGreenDark,
                    clickable = onNavigateToStats != null,
                    onClick = { onNavigateToStats?.invoke("income") },
                )

                ActionLegendRow(
                    label = "支出",
                    dotColor = ExpenseCoralDark,
                    amountText = "-$currency ${"%,.0f".format(expense)}",
                    amountColor = ExpenseCoralDark,
                    clickable = onNavigateToStats != null,
                    onClick = { onNavigateToStats?.invoke("expense") },
                )
            }
        }
    }
}

@Composable
private fun ActionLegendRow(
    label: String,
    dotColor: Color,
    amountText: String,
    amountColor: Color,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = clickable, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
                if (clickable) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "查看明細",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
