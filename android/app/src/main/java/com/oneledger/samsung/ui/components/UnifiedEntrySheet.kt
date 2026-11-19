package com.oneledger.samsung.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.oneledger.samsung.data.ledger.CreditCardEntity
import com.oneledger.samsung.data.ledger.TransactionEntity
import com.oneledger.samsung.domain.CalcEvaluator
import com.oneledger.samsung.domain.Categories
import com.oneledger.samsung.domain.ParsedEntry
import com.oneledger.samsung.domain.RewardCalculator
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 統一的新增與編輯視窗：
 * - 最上方 100% 滿寬「支出／收入」切換列
 * - 單一 description 欄位（無重複備註欄）
 * - Icon 圖示分類格
 * - 信用卡回饋選擇與海外 1.5% 淨回饋試算
 * - 嚴格等高（308.dp）底部區域：計算機 與 CameraX 下半屏拍照／結果切換零高度跳動
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEntrySheet(
    startWithPhoto: Boolean,
    editingItem: TransactionEntity?,
    prefill: ParsedEntry?,
    currency: String,
    creditCards: List<CreditCardEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        type: String,
        description: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        date: String,
        photoUri: String?,
        creditCardId: String?,
        isForeignTx: Boolean,
    ) -> Unit,
    onRequestDelete: (TransactionEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var txType by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.type ?: prefill?.type ?: "expense")
    }
    var expr by remember(editingItem, prefill) {
        mutableStateOf(
            when {
                editingItem != null -> editingItem.amount.toBigDecimal().stripTrailingZeros().toPlainString()
                prefill != null && prefill.amount > 0 -> prefill.amount.toBigDecimal().stripTrailingZeros().toPlainString()
                else -> ""
            }
        )
    }
    var description by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.description ?: prefill?.description ?: "")
    }
    var category by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.category ?: prefill?.category ?: "餐飲")
    }
    var paymentMethod by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.paymentMethod ?: prefill?.paymentMethod ?: "現金")
    }
    var date by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.date ?: prefill?.date ?: LocalDate.now().toString())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var photoUri by remember(editingItem) {
        mutableStateOf(editingItem?.photoUri)
    }
    var selectedCardId by remember(editingItem, creditCards) {
        mutableStateOf(editingItem?.creditCardId ?: creditCards.firstOrNull()?.id)
    }
    var isForeignTx by remember(editingItem, prefill) {
        mutableStateOf(editingItem?.isForeignTx ?: prefill?.isForeignTx ?: false)
    }

    // "calc" | "camera" | "result"
    var lowerMode by remember(editingItem, startWithPhoto) {
        mutableStateOf(
            when {
                !editingItem?.photoUri.isNullOrBlank() -> "result"
                startWithPhoto -> "camera"
                else -> "calc"
            }
        )
    }

    val computedAmount = CalcEvaluator.evaluate(expr)
    val hasOperator = expr.any { it in listOf('+', '-', '×', '÷') }
    val catList = if (txType == "income") Categories.incomeList else Categories.expenseList

    fun submit() {
        val amt = computedAmount ?: return
        if (amt <= 0.0) return
        onSave(
            editingItem?.id,
            txType,
            description,
            amt,
            category,
            paymentMethod,
            date,
            photoUri,
            if (paymentMethod == "信用卡") selectedCardId else null,
            if (paymentMethod == "信用卡") isForeignTx else false,
        )
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (editingItem != null) "編輯紀錄" else if (startWithPhoto) "拍照記帳" else "新增紀錄",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row {
                    if (editingItem != null) {
                        IconButton(onClick = {
                            onDismiss()
                            onRequestDelete(editingItem)
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "刪除", tint = ExpenseCoralDark)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "關閉")
                    }
                }
            }

            // 100% Full-Width 支出／收入 Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(3.dp),
            ) {
                listOf("expense" to "支出", "income" to "收入").forEach { (key, label) ->
                    val active = txType == key
                    val activeColor = if (key == "income") IncomeGreenDark else ExpenseCoralDark
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                txType = key
                                val validNames = if (key == "income") Categories.incomeList else Categories.expenseList
                                if (validNames.none { it.name == category }) category = validNames.first().name
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Amount + Calc/Camera Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                    Text(currency, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = expr.ifEmpty { "0" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (txType == "income") IncomeGreenDark else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (hasOperator && computedAmount != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "= ${"%,.0f".format(computedAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (lowerMode != "calc") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable {
                        lowerMode = if (lowerMode == "calc") {
                            if (photoUri != null) "result" else "camera"
                        } else "calc"
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            if (lowerMode == "calc") Icons.Rounded.CameraAlt else Icons.Rounded.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            if (lowerMode == "calc") (if (photoUri != null) "照片" else "拍照") else "計算機",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Single Description Input + Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("阿美自助餐，漢堡、蛋餅...", fontSize = 13.5.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                )
                val today = LocalDate.now().toString()
                val yesterday = LocalDate.now().minusDays(1).toString()
                val dateLabel = when (date) {
                    today -> "今天"
                    yesterday -> "昨天"
                    else -> {
                        val parts = date.split("-")
                        if (parts.size == 3) "${parts[1].toIntOrNull() ?: 1}/${parts[2].toIntOrNull() ?: 1}" else date
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showDatePicker = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = "選擇日期",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = dateLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (showDatePicker) {
                val initialEpochMillis = remember(date) {
                    try {
                        LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                                date = picked.toString()
                            }
                            showDatePicker = false
                        }) {
                            Text("確定", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("取消")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Category Icon Tile Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                catList.forEach { cat ->
                    val selected = category == cat.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { category = cat.name }
                            .padding(vertical = 7.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) cat.color else cat.color.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                cat.icon,
                                contentDescription = cat.name,
                                tint = if (selected) Color.White else cat.color,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }

            // Payment Method
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Categories.paymentMethods.forEach { pm ->
                    FilterChip(
                        selected = paymentMethod == pm,
                        onClick = { paymentMethod = pm },
                        label = { Text(pm, fontSize = 12.sp) },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // STRICTLY FIXED-HEIGHT (308.dp) LOWER WORKSPACE: Calculator <-> CameraX Photo Result
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(308.dp),
            ) {
                if (lowerMode == "calc") {
                    BuiltInCalculatorPad(
                        onKey = { key ->
                            expr = when (key) {
                                "C" -> ""
                                "BACK" -> expr.dropLast(1)
                                "=" -> CalcEvaluator.evaluate(expr)?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: expr
                                "+", "-", "×", "÷" -> when {
                                    expr.isEmpty() -> ""
                                    expr.last() in listOf('+', '-', '×', '÷') -> expr.dropLast(1) + key
                                    else -> expr + key
                                }
                                "." -> {
                                    val lastNum = expr.split('+', '-', '×', '÷').lastOrNull() ?: ""
                                    if (lastNum.contains(".")) expr else if (lastNum.isEmpty()) "${expr}0." else "$expr."
                                }
                                else -> if (expr == "0") key else expr + key
                            }
                        },
                        canSave = computedAmount != null && computedAmount > 0,
                        saveLabel = if (editingItem != null) "儲存變更" else "儲存",
                        onSave = ::submit,
                    )
                } else {
                    LowerHalfCameraPanel(
                        mode = lowerMode,
                        photoUri = photoUri,
                        canSave = computedAmount != null && computedAmount > 0,
                        onSwitchMode = { lowerMode = it },
                        onPhotoCaptured = { path ->
                            photoUri = path
                            lowerMode = "result"
                        },
                        onRemovePhoto = {
                            photoUri = null
                            lowerMode = "camera"
                        },
                        onSave = ::submit,
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInCalculatorPad(
    onKey: (String) -> Unit,
    canSave: Boolean,
    saveLabel: String,
    onSave: () -> Unit,
) {
    val view = LocalView.current
    val hapticClick: (String) -> Unit = { key ->
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onKey(key)
    }

    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", ".", "+"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                row.forEach { key ->
                    val isOp = key in listOf("÷", "×", "-", "+")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOp || key == "C") MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { hapticClick(key) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Bottom Row: Backspace | = | Save (span 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { hapticClick("BACK") },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "退格", modifier = Modifier.size(19.dp))
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { hapticClick("=") },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("=", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clickable(
                        enabled = canSave,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onSave()
                        }
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(saveLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * 拍照區塊：左側 1:1 照片/取景預覽，右側直排控制按鈕（即時取景/拍照結果、垃圾桶、確認儲存）
 */
@Composable
private fun LowerHalfCameraPanel(
    mode: String,
    photoUri: String?,
    canSave: Boolean,
    onSwitchMode: (String) -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E12))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左側 1:1 正方形相機取景／照片檢視容器
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF171920)),
            contentAlignment = Alignment.Center,
        ) {
            if (mode == "camera") {
                if (hasCameraPermission) {
                    val previewView = remember { PreviewView(context) }
                    DisposableEffect(lifecycleOwner) {
                        val future = ProcessCameraProvider.getInstance(context)
                        val listener = Runnable {
                            try {
                                val provider = future.get()
                                val preview = Preview.Builder()
                                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                    .build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            } catch (_: Exception) {
                                // Camera unavailable on emulator
                            }
                        }
                        future.addListener(listener, ContextCompat.getMainExecutor(context))
                        onDispose {
                            try {
                                future.get().unbindAll()
                            } catch (_: Exception) {
                            }
                        }
                    }
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("需要相機權限", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("授權", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "已拍攝的消費照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "尚未拍攝照片\n請切換至「即時取景」",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // 右側 直排操作按鈕面板
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 上方：即時取景 與 拍照結果 切換
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf("camera" to "即時取景", "result" to "拍照結果").forEach { (k, label) ->
                    val active = mode == k
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Color.White else Color.Transparent)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSwitchMode(k)
                            }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) Color(0xFF111318) else Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // 中間：相機快門按鈕 或 刪除照片垃圾桶按鈕
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (mode == "camera") {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (!hasCameraPermission) {
                                    permLauncher.launch(Manifest.permission.CAMERA)
                                    return@clickable
                                }
                                takeLocalPhoto(context, imageCapture, onPhotoCaptured)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = "拍照",
                            tint = Color(0xFF111318),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text("快門拍攝", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                } else {
                    if (photoUri != null) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onRemovePhoto()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(ExpenseCoralDark.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "刪除照片",
                                tint = ExpenseCoralDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text("刪除照片", fontSize = 11.sp, color = ExpenseCoralDark)
                    } else {
                        Spacer(Modifier.height(44.dp))
                    }
                }
            }

            // 下方：確認儲存
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clickable(
                        enabled = canSave,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onSave()
                        }
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("確認儲存", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun takeLocalPhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (String) -> Unit,
) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "OneLedger_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/OneLedger")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uriStr = outputFileResults.savedUri?.toString()
                if (uriStr != null) {
                    onSaved(uriStr)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                try {
                    val dir = File(context.filesDir, "photos").apply { mkdirs() }
                    val fallbackFile = File(dir, "tx_${System.currentTimeMillis()}.jpg")
                    val fallbackOptions = ImageCapture.OutputFileOptions.Builder(fallbackFile).build()
                    imageCapture.takePicture(
                        fallbackOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(res: ImageCapture.OutputFileResults) {
                                onSaved(fallbackFile.absolutePath)
                            }
                            override fun onError(e: ImageCaptureException) {}
                        }
                    )
                } catch (_: Exception) {}
            }
        },
    )
}
