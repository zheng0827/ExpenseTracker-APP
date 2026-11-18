package com.oneledger.samsung.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import java.time.YearMonth
import kotlin.math.abs

/**
 * 統計圖表 1：消費趨勢走勢圖（支援「每日」與「整月累計」切換）
 */
@Composable
fun SpendingTrendChartCard(
    month: YearMonth,
    transactions: List<TransactionEntity>,
    currency: String,
) {
    var cumulativeMode by remember { mutableStateOf(false) }
    val daysCount = month.lengthOfMonth()
    val monthPrefix = month.toString()

    val dailyTotals = remember(month, transactions) {
        val arr = DoubleArray(daysCount)
        transactions.filter { it.type != "income" && it.date.startsWith(monthPrefix) }.forEach { tx ->
            val d = tx.date.takeLast(2).toIntOrNull()
            if (d != null && d in 1..daysCount) arr[d - 1] += tx.amount
        }
        arr
    }

    val series = remember(dailyTotals, cumulativeMode) {
        if (!cumulativeMode) dailyTotals.toList()
        else {
            var sum = 0.0
            dailyTotals.map { sum += it; sum }
        }
    }

    val totalExpense = dailyTotals.sum()
    val avgDaily = totalExpense / daysCount.coerceAtLeast(1)
    val peakIdx = dailyTotals.indices.maxByOrNull { dailyTotals[it] } ?: 0
    val peakVal = dailyTotals.getOrElse(peakIdx) { 0.0 }
    val maxVal = (series.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val guideColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("消費趨勢走勢", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))
                        .padding(3.dp),
                ) {
                    SmallToggleChip("每日", !cumulativeMode) { cumulativeMode = false }
                    SmallToggleChip("整月累計", cumulativeMode) { cumulativeMode = true }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 12f
                val bottomPad = 10f
                val plotH = h - topPad - bottomPad

                listOf(0f, 0.5f, 1f).forEach { r ->
                    val y = topPad + plotH * r
                    drawLine(color = guideColor.copy(alpha = 0.4f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                }

                val pts = series.mapIndexed { i, v ->
                    val x = (i.toFloat() / (series.size - 1).coerceAtLeast(1)) * w
                    val y = topPad + plotH - ((v / maxVal).toFloat() * plotH)
                    Offset(x, y)
                }

                if (pts.isNotEmpty()) {
                    val linePath = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(pts.last().x, topPad + plotH)
                        lineTo(pts.first().x, topPad + plotH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ExpenseCoralDark.copy(alpha = 0.28f), Color.Transparent),
                            startY = topPad,
                            endY = topPad + plotH,
                        ),
                    )
                    drawPath(
                        path = linePath,
                        color = ExpenseCoralDark,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                    )
                    if (!cumulativeMode) {
                        pts.forEachIndexed { i, pt ->
                            if (series[i] > 0) drawCircle(color = ExpenseCoralDark, radius = 3.dp.toPx(), center = pt)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "日均支出 $currency ${"%,.0f".format(avgDaily)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "最高單日 ${peakIdx + 1} 日（$currency ${"%,.0f".format(peakVal)}）",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 統計圖表 2：近 6 個月收支結餘三折線圖（點擊月份可檢視精確數值）
 */
@Composable
fun MonthlyBalanceLineChartCard(
    selectedMonth: YearMonth,
    transactions: List<TransactionEntity>,
    currency: String,
) {
    val months = remember(selectedMonth) {
        (5 downTo 0).map { selectedMonth.minusMonths(it.toLong()) }
    }
    var focusIdx by remember(selectedMonth) { mutableIntStateOf(5) }

    val stats = remember(months, transactions) {
        months.map { ym ->
            val prefix = ym.toString()
            val txs = transactions.filter { it.date.startsWith(prefix) }
            val inc = txs.filter { it.type == "income" }.sumOf { it.amount }
            val exp = txs.filter { it.type != "income" }.sumOf { it.amount }
            Triple(inc, exp, inc - exp)
        }
    }

    val allVals = stats.flatMap { listOf(it.first, it.second, it.third) }
    val maxVal = (allVals.maxOrNull() ?: 1000.0).coerceAtLeast(1000.0)
    val minVal = (allVals.minOrNull() ?: 0.0).coerceAtMost(0.0)
    val span = (maxVal - minVal).coerceAtLeast(1.0)
    val accent = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outline
    val focused = stats.getOrElse(focusIdx) { Triple(0.0, 0.0, 0.0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("近 6 個月收支結餘折線圖", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${months[focusIdx].monthValue}月", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("收 ${"%,.0f".format(focused.first)}", fontSize = 12.sp, color = IncomeGreenDark, fontWeight = FontWeight.SemiBold)
                Text("支 ${"%,.0f".format(focused.second)}", fontSize = 12.sp, color = ExpenseCoralDark, fontWeight = FontWeight.SemiBold)
                Text(
                    "餘 ${if (focused.third < 0) "-" else ""}$currency ${"%,.0f".format(abs(focused.third))}",
                    fontSize = 12.sp,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                val padX = 14.dp.toPx()
                val plotW = size.width - padX * 2
                val topPad = 10.dp.toPx()
                val plotH = size.height - topPad * 2

                fun mapX(i: Int) = padX + (i.toFloat() / (stats.size - 1).coerceAtLeast(1)) * plotW
                fun mapY(v: Double) = topPad + plotH - (((v - minVal) / span).toFloat() * plotH)

                listOf(0f, 0.5f, 1f).forEach { r ->
                    val y = topPad + plotH * r
                    drawLine(guideColor.copy(alpha = 0.4f), Offset(padX, y), Offset(size.width - padX, y), 1f)
                }

                fun drawLineSeries(color: Color, selector: (Triple<Double, Double, Double>) -> Double) {
                    val path = Path()
                    stats.forEachIndexed { i, item ->
                        val pt = Offset(mapX(i), mapY(selector(item)))
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    drawPath(path, color = color, style = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round))
                    stats.forEachIndexed { i, item ->
                        drawCircle(
                            color = color,
                            radius = if (i == focusIdx) 4.5.dp.toPx() else 2.8.dp.toPx(),
                            center = Offset(mapX(i), mapY(selector(item))),
                        )
                    }
                }

                drawLineSeries(IncomeGreenDark) { it.first }
                drawLineSeries(ExpenseCoralDark) { it.second }
                drawLineSeries(accent) { it.third }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                months.forEachIndexed { i, ym ->
                    val selected = i == focusIdx
                    Text(
                        text = "${ym.monthValue}月",
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { focusIdx = i }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallToggleChip(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) ExpenseCoralDark else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                RoundedCornerShape(99.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
