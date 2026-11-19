package com.oneledger.samsung.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oneledger.samsung.domain.Categories
import com.oneledger.samsung.domain.ParsedEntry
import com.oneledger.samsung.ui.theme.ExpenseCoralDark
import com.oneledger.samsung.ui.theme.IncomeGreenDark
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 底部精簡 1/3 高度視窗：
 * - mode == "voice"：進場震動並啟動原生 Android SpeechRecognizer (zh-TW)，提供停止／取消／送出 LLM 分析
 * - mode == "text"：輸入自然語句，送出 LLM 正規化為單一 description 並可一鍵入帳或進階編輯
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmQuickSheet(
    mode: String, // "voice" | "text"
    currency: String,
    onParse: suspend (String) -> ParsedEntry,
    onConfirmSave: (ParsedEntry) -> Unit,
    onOpenInEditor: (ParsedEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sentence by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var statusText by remember(mode) {
        mutableStateOf(
            if (mode == "voice") "可以開始講話，說完請按「停止」或「送出」"
            else "輸入一句話，例如：「今天去阿美自助餐買早餐 180 元，吃了漢堡蛋餅」"
        )
    }
    var analyzing by remember { mutableStateOf(false) }
    var parsed by remember { mutableStateOf<ParsedEntry?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun startSpeech() {
        if (recognizer == null) {
            statusText = "此裝置未安裝語音辨識引擎，請直接輸入文字後送出"
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        listening = true
        statusText = "正在聆聽中，可以開始講話..."
        recognizer.startListening(intent)
    }

    val audioPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeech() else statusText = "未授權麥克風，可直接輸入文字測試"
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                statusText = "收音結束，可按「重說」或直接送出分析"
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    sentence = text
                    scope.launch {
                        analyzing = true
                        statusText = "已辨識語音，正在自動拆分..."
                        parsed = onParse(text)
                        analyzing = false
                        statusText = "已完成自動拆分，請確認或儲存"
                    }
                } else {
                    statusText = "收音結束，未識別到文字，可按「重說」或手動輸入"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) sentence = text
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            recognizer?.stopListening()
            recognizer?.destroy()
        }
    }

    LaunchedEffect(mode) {
        if (mode == "voice") {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) startSpeech() else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (mode == "voice") Icons.Rounded.Mic else Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (listening) ExpenseCoralDark else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (mode == "voice") "語音記帳（LLM 分析）" else "文字記帳（LLM 分析）",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "關閉")
                }
            }

            Text(
                text = statusText,
                fontSize = 12.5.sp,
                color = if (listening) ExpenseCoralDark else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (parsed == null) {
                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    placeholder = { Text("例如：今天去阿美自助餐買早餐 180 元，吃了漢堡蛋餅奶茶") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!analyzing && sentence.isNotBlank()) {
                            recognizer?.stopListening()
                            listening = false
                            scope.launch {
                                analyzing = true
                                parsed = onParse(sentence)
                                analyzing = false
                            }
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                        Text("取消")
                    }
                    if (mode == "voice") {
                        OutlinedButton(
                            onClick = {
                                if (listening) {
                                    recognizer?.stopListening()
                                    listening = false
                                } else startSpeech()
                            },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (listening) "停止" else "重說")
                        }
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !analyzing && sentence.isNotBlank(),
                        onClick = {
                            recognizer?.stopListening()
                            listening = false
                            scope.launch {
                                analyzing = true
                                parsed = onParse(sentence)
                                analyzing = false
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (analyzing) "分析拆分中..." else "送出")
                    }
                }
            } else {
                val res = parsed!!
                val catInfo = Categories.find(res.category)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(catInfo.color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(catInfo.icon, contentDescription = null, tint = catInfo.color)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(res.description, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { showDatePicker = true },
                        ) {
                            Icon(
                                Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "${res.date} · ${res.category} · ${res.paymentMethod}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "${if (res.type == "income") "+" else "-"}$currency ${"%,.0f".format(res.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (res.type == "income") IncomeGreenDark else MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (showDatePicker) {
                    val initialEpochMillis = remember(res.date) {
                        try {
                            LocalDate.parse(res.date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
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
                                    parsed = res.copy(date = picked.toString())
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

                Spacer(Modifier.height(2.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { parsed = null }, shape = RoundedCornerShape(16.dp)) {
                        Text("重寫")
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenInEditor(res)
                        },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("編輯")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = res.amount > 0,
                        onClick = {
                            onConfirmSave(res)
                            onDismiss()
                        },
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("確認入帳")
                    }
                }
            }
        }
    }
}
