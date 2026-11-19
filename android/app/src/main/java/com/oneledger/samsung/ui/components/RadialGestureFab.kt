package com.oneledger.samsung.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class RadialItem(
    val mode: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val dxDp: Float,
    val dyDp: Float,
)

/**
 * 右下角核心記帳按鈕（專為三星單手操作設計，無側邊返回衝突）：
 * - 短按：直接開啟標準手動「新增紀錄」（Icon 分類＋計算機）
 * - 長按並往左上(330°)／正上(0°)／右上(30°)滑動放開：分別進入 語音(LLM) / 文字(LLM) / 拍照記帳
 */
@Composable
fun RadialGestureFab(
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        listOf(
            RadialItem("voice", "語音記帳（LLM）", "語音", Icons.Rounded.Mic, -104f, -66f),
            RadialItem("text", "文字記帳（LLM）", "文字", Icons.Rounded.Forum, -52f, -118f),
            RadialItem("photo", "拍照記帳", "拍照", Icons.Rounded.CameraAlt, 4f, -106f),
        )
    }

    var menuOpen by remember { mutableStateOf(false) }
    var hoveredMode by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.size(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (menuOpen) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .wrapContentSize(align = Alignment.Center, unbounded = true),
                contentAlignment = Alignment.Center,
            ) {
                val activeLabel = items.firstOrNull { it.mode == hoveredMode }?.label
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color(0xFF1C1F26),
                    modifier = Modifier
                        .offset(x = (-32).dp, y = (-168).dp)
                        .shadow(8.dp, RoundedCornerShape(99.dp)),
                ) {
                    Text(
                        text = activeLabel?.let { "放開進入：$it" } ?: "滑向任一方向或點選模式",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                items.forEach { item ->
                    val isSelected = hoveredMode == item.mode
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = with(density) { item.dxDp.dp.toPx().roundToInt() },
                                    y = with(density) { item.dyDp.dp.toPx().roundToInt() },
                                )
                            }
                            .size(if (isSelected) 60.dp else 54.dp)
                            .clickable {
                                menuOpen = false
                                hoveredMode = null
                                onSelectMode(item.mode)
                            },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp),
                            )
                            Text(
                                text = item.shortLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(10.dp, RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
                .pointerInput(items, density) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (menuOpen) {
                            menuOpen = false
                            hoveredMode = null
                            return@awaitEachGesture
                        }

                        var isLongPress = false
                        var dragDx = 0f
                        var dragDy = 0f
                        val startPos = down.position

                        val holdJob = scope.launch {
                            delay(220)
                            isLongPress = true
                            menuOpen = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                holdJob.cancel()
                                if (!isLongPress) {
                                    onSelectMode("manual")
                                } else if (hoveredMode != null) {
                                    val chosen = hoveredMode!!
                                    menuOpen = false
                                    hoveredMode = null
                                    onSelectMode(chosen)
                                }
                                break
                            }

                            dragDx = change.position.x - startPos.x
                            dragDy = change.position.y - startPos.y

                            if (!isLongPress && dragDy < with(density) { (-20).dp.toPx() }) {
                                holdJob.cancel()
                                isLongPress = true
                                menuOpen = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            if (isLongPress) {
                                val distPx = hypot(dragDx, dragDy)
                                if (distPx < with(density) { 28.dp.toPx() }) {
                                    hoveredMode = null
                                } else {
                                    var best: String? = null
                                    var bestDist = Float.MAX_VALUE
                                    for (item in items) {
                                        val ix = with(density) { item.dxDp.dp.toPx() }
                                        val iy = with(density) { item.dyDp.dp.toPx() }
                                        val d = hypot(dragDx - ix, dragDy - iy)
                                        if (d < bestDist) {
                                            bestDist = d
                                            best = item.mode
                                        }
                                    }
                                    hoveredMode = if (bestDist < with(density) { 74.dp.toPx() }) best else null
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (menuOpen) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = "新增記帳，長按滑動選擇模式",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
