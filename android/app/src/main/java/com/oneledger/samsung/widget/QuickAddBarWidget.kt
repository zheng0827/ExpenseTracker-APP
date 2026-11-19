package com.oneledger.samsung.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oneledger.samsung.MainActivity
import com.oneledger.samsung.QuickCaptureActivity
import com.oneledger.samsung.R
import com.oneledger.samsung.data.SecurePrefs
import com.oneledger.samsung.data.ledger.LedgerDatabase
import java.time.LocalDate

/**
 * 三星 One UI 風格 Jetpack Glance 4x1 桌面小工具：
 * - 顯示今日支出
 * - 放大按鈕與圖示（加號、語音麥克風、相機拍照），點擊直接浮動彈出小視窗記帳
 * - 支援設定自訂小工具底色
 */
class QuickAddBarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var todayExp = 0.0
        var currency = "NT$"
        var bgStyle = "obsidian"

        try {
            val appContext = context.applicationContext ?: context
            val db = LedgerDatabase.getInstance(appContext)
            val prefs = SecurePrefs(appContext)
            currency = prefs.currency
            bgStyle = prefs.widgetBackgroundStyle
            val today = LocalDate.now().toString()
            todayExp = db.transactionDao().getByDate(today)
                .filter { it.type != "income" }
                .sumOf { it.amount }
        } catch (_: Exception) {
            // 避免冷啟動拋出例外
        }

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
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(ColorProvider(bgColor))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                // 左側：今日支出金額（點擊可進入完整 App）
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Text(
                        text = "今日支出",
                        style = TextStyle(color = ColorProvider(subTextColor), fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "$currency ${"%,.0f".format(todayExp)}",
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                // 右側：放大的圖示按鈕（加號、語音、相機）直接喚起 QuickCaptureActivity
                MiniBarIconButton(
                    iconRes = R.drawable.ic_widget_add,
                    label = "快速記帳",
                    mode = "manual",
                    bgColor = Color(0xFF0381FE)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                MiniBarIconButton(
                    iconRes = R.drawable.ic_widget_mic,
                    label = "語音記帳",
                    mode = "voice",
                    bgColor = pillBgColor
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                MiniBarIconButton(
                    iconRes = R.drawable.ic_widget_camera,
                    label = "拍照記帳",
                    mode = "photo",
                    bgColor = pillBgColor
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MiniBarIconButton(
    iconRes: Int,
    label: String,
    mode: String,
    bgColor: Color,
) {
    Row(
        modifier = GlanceModifier
            .size(38.dp)
            .cornerRadius(14.dp)
            .background(ColorProvider(bgColor))
            .clickable(
                actionStartActivity<QuickCaptureActivity>(
                    actionParametersOf(WIDGET_LAUNCH_MODE_KEY to mode)
                )
            ),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            modifier = GlanceModifier.size(19.dp),
        )
    }
}

class QuickAddBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddBarWidget()
}
