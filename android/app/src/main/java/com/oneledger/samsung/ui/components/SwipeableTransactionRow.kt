package com.oneledger.samsung.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.Categories
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import java.io.File
import kotlin.math.roundToInt

/**
 * 支援「往左滑露出編輯與刪除按鈕」的交易列（不干擾直向捲動）
 */
@Composable
fun SwipeableTransactionRow(
    item: TransactionEntity,
    currency: String,
    subtitlePrefix: String? = null,
    isOpen: Boolean,
    onSwipeStateChange: (String?) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onRequestDelete: (TransactionEntity) -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { 132.dp.toPx() }
    var dragOffset by remember(item.id, isOpen) {
        mutableFloatStateOf(if (isOpen) -actionWidthPx else 0f)
    }

    val isIncome = item.type == "income"
    val catInfo = Categories.find(item.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // 右側隱藏操作區（編輯 / 刪除）
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(132.dp)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        onSwipeStateChange(null)
                        onEdit(item)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Edit, contentDescription = "編輯", tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("編輯", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ExpenseCoralDark)
                    .clickable {
                        onSwipeStateChange(null)
                        onRequestDelete(item)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Delete, contentDescription = "刪除", tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("刪除", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 前景可拖曳列
        Row(
            modifier = Modifier
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(item.id, actionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset < -actionWidthPx * 0.4f) {
                                dragOffset = -actionWidthPx
                                onSwipeStateChange(item.id)
                            } else {
                                dragOffset = 0f
                                onSwipeStateChange(null)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                    )
                }
                .clickable {
                    if (isOpen) {
                        onSwipeStateChange(null)
                    } else {
                        onEdit(item)
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catInfo.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = File(item.photoUri),
                        contentDescription = item.description,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp),
                    )
                } else {
                    Icon(catInfo.icon, contentDescription = item.category, tint = catInfo.color, modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "${subtitlePrefix ?: item.time} · ${item.category} · ${item.paymentMethod}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.rewardPoints > 0) {
                        Text(
                            text = "+${"%.1f".format(item.rewardPoints)}${item.rewardUnit}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Text(
                text = "${if (isIncome) "+" else "-"}$currency ${"%,.0f".format(item.amount)}",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeGreenDark else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun ConfirmDeleteTransactionDialog(
    target: TransactionEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    if (target == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("確定要刪除這筆紀錄嗎？", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Text(
                "「${target.description}」（${if (target.type == "income") "+" else "-"}$currency ${"%,.0f".format(target.amount)}）刪除後將無法復原。",
                fontSize = 14.sp,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoralDark),
                onClick = {
                    onConfirmDelete(target.id)
                    onDismiss()
                },
            ) {
                Text("確認刪除", color = Color.White)
            }
        },
    )
}
