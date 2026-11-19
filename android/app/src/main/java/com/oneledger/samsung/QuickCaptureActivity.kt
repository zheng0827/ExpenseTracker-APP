package com.oneledger.samsung

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.oneledger.samsung.domain.ParsedEntry
import com.oneledger.samsung.ui.MainViewModel
import com.oneledger.samsung.ui.components.LlmQuickSheet
import com.oneledger.samsung.ui.components.UnifiedEntrySheet
import com.oneledger.samsung.ui.theme.OneLedgerTheme

/**
 * 桌面小工具快速啟動浮動視窗 (Transparent Floating Dialog Activity)
 * 點擊桌面小工具按鈕時，無需開啟完整 App 導航列，直接於桌面浮現記帳面板。
 */
class QuickCaptureActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialMode = intent?.getStringExtra("launch_mode") ?: "manual"

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currency by viewModel.currency.collectAsState()
            val creditCards by viewModel.creditCards.collectAsState()

            var activeMode by remember { mutableStateOf(initialMode) }
            var prefillEntry by remember { mutableStateOf<ParsedEntry?>(null) }
            var startPhoto by remember { mutableStateOf(initialMode == "photo" || initialMode == "camera") }

            OneLedgerTheme(themeMode = themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { finish() }
                        )
                ) {
                    when (activeMode) {
                        "voice", "text" -> {
                            LlmQuickSheet(
                                mode = activeMode,
                                currency = currency,
                                onParse = viewModel::parseNaturalSentence,
                                onConfirmSave = { parsed ->
                                    viewModel.saveTransaction(
                                        type = parsed.type,
                                        description = parsed.description,
                                        amount = parsed.amount,
                                        category = parsed.category,
                                        paymentMethod = parsed.paymentMethod,
                                        date = parsed.date,
                                        isForeignTx = parsed.isForeignTx,
                                    )
                                    finish()
                                },
                                onOpenInEditor = { parsed ->
                                    prefillEntry = parsed
                                    startPhoto = false
                                    activeMode = "manual"
                                },
                                onDismiss = { finish() },
                            )
                        }
                        else -> {
                            UnifiedEntrySheet(
                                startWithPhoto = startPhoto,
                                editingItem = null,
                                prefill = prefillEntry,
                                currency = currency,
                                creditCards = creditCards,
                                onDismiss = { finish() },
                                onSave = { id, type, desc, amt, cat, pm, date, photo, cardId, isForeign ->
                                    viewModel.saveTransaction(
                                        id = id,
                                        type = type,
                                        description = desc,
                                        amount = amt,
                                        category = cat,
                                        paymentMethod = pm,
                                        date = date,
                                        photoUri = photo,
                                        creditCardId = cardId,
                                        isForeignTx = isForeign,
                                    )
                                    finish()
                                },
                                onRequestDelete = { finish() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}
