package com.oneledger.samsung.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oneledger.samsung.MainActivity
import com.oneledger.samsung.data.SecurePrefs
import com.oneledger.samsung.data.ledger.LedgerDatabase
import java.time.YearMonth

val WIDGET_LAUNCH_MODE_KEY = ActionParameters.Key<String>("launch_mode")

/**
 * 三星 One UI 風格 Jetpack Glance 4x2 桌面小工具：
 * 直接讀取本機 SQLite（ledger.db）顯示本月結餘、收入與支出，並提供 4 顆快捷記帳入口。
 */
class BalanceQuickWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = LedgerDatabase.getInstance(context)
        val prefs = SecurePrefs(context)
        val ym = YearMonth.now()
        val monthTxs = db.transactionDao().getByMonth("${ym}%")
        val income = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
        val expense = monthTxs.filter { it.type != "income" }.sumOf { it.amount }
        val balance = income - expense
        val currency = prefs.currency

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(ColorProvider(Color(0xFF17181C)))
                    .padding(14.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "${ym.monthValue} 月結餘",
                            style = TextStyle(color = ColorProvider(Color(0xFF989FA9)), fontSize = 12.sp),
                        )
                        Text(
                            text = "$currency ${"%,.0f".format(balance)}",
                            style = TextStyle(
                                color = ColorProvider(if (balance < 0) Color(0xFFFF6B60) else Color(0xFFFFFFFF)),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text(
                            text = "收 +${"%,.0f".format(income)}",
                            style = TextStyle(color = ColorProvider(Color(0xFF36C978)), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                        Text(
                            text = "支 -${"%,.0f".format(expense)}",
                            style = TextStyle(color = ColorProvider(Color(0xFFFF6B60)), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    WidgetQuickPill("記一筆", "manual", GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    WidgetQuickPill("語音", "voice", GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    WidgetQuickPill("文字", "text", GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    WidgetQuickPill("拍照", "photo", GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetQuickPill(label: String, mode: String, modifier: GlanceModifier) {
    Column(
        modifier = modifier
            .cornerRadius(14.dp)
            .background(ColorProvider(Color(0xFF262A34)))
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(WIDGET_LAUNCH_MODE_KEY to mode)
                )
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color(0xFFF5F6F8)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

class BalanceQuickWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceQuickWidget()
}
