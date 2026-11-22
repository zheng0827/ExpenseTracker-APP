package com.oneledger.samsung.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oneledger.samsung.MainActivity
import com.oneledger.samsung.QuickCaptureActivity
import com.oneledger.samsung.R
import com.oneledger.samsung.data.SecurePrefs
import com.oneledger.samsung.data.ledger.LedgerDatabase
import java.time.YearMonth
import kotlin.math.abs

val WIDGET_LAUNCH_MODE_KEY = ActionParameters.Key<String>("launch_mode")

/**
 * 三星 One UI 風格 Jetpack Glance 4x2 桌面小工具：
 * - 支援設定自訂小工具底色（曜石深黑 / AMOLED 純黑 / 毛玻璃 / 簡約淺白）
 * - 內嵌收支雙色甜甜圈圖（綠色收入 vs 珊瑚紅支出，無藍色）
 * - 大字體顯示結餘數值與收支
 * - 點擊「+」、「語音」、「相機」按鈕直接喚起 QuickCaptureActivity 浮動視窗，不開整支 App
 */
class BalanceQuickWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ym = YearMonth.now()
        var income = 0.0
        var expense = 0.0
        var currency = "NT$"
        var bgStyle = "obsidian"

        try {
            val appContext = context.applicationContext ?: context
            val db = LedgerDatabase.getInstance(appContext)
            val prefs = SecurePrefs(appContext)
            currency = prefs.currency
            bgStyle = prefs.widgetBackgroundStyle
            val monthTxs = db.transactionDao().getByMonth("${ym}%")
            income = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
            expense = monthTxs.filter { it.type != "income" }.sumOf { it.amount }
        } catch (_: Exception) {
            // 避免冷啟動例外
        }

        val balance = income - expense
        val donutBitmap = createDonutBitmap(income, expense)
        val bgColor = when (bgStyle) {
            "amoled" -> Color(0xFF000000)
            "glass" -> Color(0xB81A1C24)
            "light" -> Color(0xF5F0F2F5)
            else -> Color(0xE614161C)
        }
        val isLight = bgStyle == "light"
        val textColor = if (isLight) Color(0xFF1B1D22) else Color(0xFFFFFFFF)
        val subTextColor = if (isLight) Color(0xFF6B7280) else Color(0xFF98A0AC)
        val pillBgColor = if (isLight) Color(0xFFE2E5EC) else Color(0xFF252934)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(ColorProvider(bgColor))
                    .padding(14.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                // 上半部：甜甜圈、結餘與收入支出（點擊開啟 App 主程式）
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(donutBitmap),
                        contentDescription = "收支甜甜圈圖",
                        modifier = GlanceModifier.size(52.dp),
                    )

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "${ym.monthValue} 月結餘",
                            style = TextStyle(color = ColorProvider(subTextColor), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        )
                        Text(
                            text = "${if (balance < 0) "-" else ""}$currency ${"%,.0f".format(abs(balance))}",
                            style = TextStyle(
                                color = ColorProvider(if (balance < 0) Color(0xFFFF6359) else textColor),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text(
                            text = "+$currency ${"%,.0f".format(income)}",
                            style = TextStyle(color = ColorProvider(Color(0xFF2EB872)), fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "-$currency ${"%,.0f".format(expense)}",
                            style = TextStyle(color = ColorProvider(Color(0xFFFF6359)), fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                // 下半部：放大快捷按鈕（加號、語音、相機圖示）直接喚起浮動記帳面板
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    WidgetIconButton(
                        iconRes = R.drawable.ic_widget_add,
                        label = "快速記帳",
                        mode = "manual",
                        bgColor = Color(0xFF0381FE),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    WidgetIconButton(
                        iconRes = R.drawable.ic_widget_mic,
                        label = "語音記帳",
                        mode = "voice",
                        bgColor = pillBgColor,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    WidgetIconButton(
                        iconRes = R.drawable.ic_widget_camera,
                        label = "拍照記帳",
                        mode = "photo",
                        bgColor = pillBgColor,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }

    private fun createDonutBitmap(income: Double, expense: Double): Bitmap {
        val size = 160
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val total = income + expense
        val strokeWidth = 24f
        val oval = RectF(strokeWidth / 2f + 4f, strokeWidth / 2f + 4f, size - strokeWidth / 2f - 4f, size - strokeWidth / 2f - 4f)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33888888.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.BUTT
        }
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        if (total > 0.0) {
            val incomeSweep = ((income / total) * 360.0).toFloat()
            val expenseSweep = ((expense / total) * 360.0).toFloat()

            val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF2EB872.toInt() // Income Green
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.BUTT
            }
            val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFF6359.toInt() // Expense Coral
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.BUTT
            }

            if (incomeSweep > 0f) {
                canvas.drawArc(oval, -90f, incomeSweep, false, incomePaint)
            }
            if (expenseSweep > 0f) {
                canvas.drawArc(oval, -90f + incomeSweep, expenseSweep, false, expensePaint)
            }
        }
        return bitmap
    }
}

@androidx.compose.runtime.Composable
private fun WidgetIconButton(
    iconRes: Int,
    label: String,
    mode: String,
    bgColor: Color,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier
            .cornerRadius(16.dp)
            .background(ColorProvider(bgColor))
            .clickable(
                actionStartActivity<QuickCaptureActivity>(
                    actionParametersOf(WIDGET_LAUNCH_MODE_KEY to mode)
                )
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            modifier = GlanceModifier.size(20.dp),
        )
    }
}

class BalanceQuickWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceQuickWidget()
}
