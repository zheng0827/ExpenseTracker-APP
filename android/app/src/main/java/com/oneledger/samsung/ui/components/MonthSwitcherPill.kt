package com.oneledger.samsung.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.time.YearMonth

/**
 * 支援「左/中/右三個獨立區塊」的三星 One UI 月份年份切換膠囊：
 * - 左區塊：切換至上個月
 * - 中間區塊：點選滑出年份與 12 個月份的快速選擇面板，支援自選年份與指定月份快速跳轉
 * - 右區塊：切換至下個月
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSwitcherPill(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        ) {
            // 區塊 1：上個月
            IconButton(
                onClick = onPrev,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.ChevronLeft,
                    contentDescription = "上個月",
                    modifier = Modifier.size(18.dp),
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // 區塊 2：中間點選彈出年份與月份清單
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable { showSheet = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${month.year} 年 ${month.monthValue} 月",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = "自選年月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // 區塊 3：下個月
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "下個月",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (showSheet) {
        var browsingYear by remember(month.year) { mutableIntStateOf(month.year) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header: 標題與關閉
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "快速跳轉年份與月份",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showSheet = false }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "關閉")
                    }
                }

                // 年份選擇器（帶左右切換按鈕）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { browsingYear-- },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一年")
                    }

                    Text(
                        text = "$browsingYear 年",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    IconButton(
                        onClick = { browsingYear++ },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "後一年")
                    }
                }

                // 12 個月份格狀清單（4 列 x 3 欄）
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (col in 1..3) {
                                val m = row * 3 + col
                                val isSelected = browsingYear == month.year && m == month.monthValue
                                val isCurrentActualMonth = browsingYear == YearMonth.now().year && m == YearMonth.now().monthValue

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectMonth(YearMonth.of(browsingYear, m))
                                            showSheet = false
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isCurrentActualMonth -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$m 月",
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected || isCurrentActualMonth) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                isCurrentActualMonth -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 快速按鈕：回到本月
                OutlinedButton(
                    onClick = {
                        val currentYm = YearMonth.now()
                        onSelectMonth(currentYm)
                        showSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("回到本月（${YearMonth.now().year} 年 ${YearMonth.now().monthValue} 月）")
                }
            }
        }
    }
}
