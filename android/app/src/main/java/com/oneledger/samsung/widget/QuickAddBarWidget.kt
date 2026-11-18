package com.oneledger.samsung.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oneledger.samsung.MainActivity
import com.oneledger.samsung.data.SecurePrefs
import com.oneledger.samsung.data.ledger.LedgerDatabase
import java.time.LocalDate

/**
 * 三星 One UI 風格 Jetpack Glance 4x1 桌面小工具：顯示今日支出＋一鍵快速記帳按鈕。
 */
class QuickAddBarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = LedgerDatabase.getInstance(context)
        val prefs = SecurePrefs(context)
        val today = LocalDate.now().toString()
        val todayExp = db.transactionDao().getByDate(today)
            .filter { it.type != "income" }
            .sumOf { it.amount }
        val currency = prefs.currency

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(ColorProvider(Color(0xFF17181C)))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "今日支出",
                        style = TextStyle(color = ColorProvider(Color(0xFF989FA9)), fontSize = 11.sp),
                    )
                    Text(
                        text = "$currency ${"%,.0f".format(todayExp)}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFFFFF)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                MiniBarButton("+", "manual", Color(0xFF0381FE))
                Spacer(modifier = GlanceModifier.width(6.dp))
                MiniBarButton("語", "voice", Color(0xFF262A34))
                Spacer(modifier = GlanceModifier.width(6.dp))
                MiniBarButton("拍", "photo", Color(0xFF262A34))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MiniBarButton(text: String, mode: String, bg: Color) {
    Row(
        modifier = GlanceModifier
            .cornerRadius(14.dp)
            .background(ColorProvider(bg))
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(WIDGET_LAUNCH_MODE_KEY to mode)
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
        )
    }
}

class QuickAddBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddBarWidget()
}
