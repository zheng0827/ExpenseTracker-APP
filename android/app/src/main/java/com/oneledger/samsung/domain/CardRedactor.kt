package com.oneledger.samsung.domain

data class RedactionResult(
    val redactedText: String,
    val maskedCount: Int,
)

/**
 * 本地端隱私去識別引擎：在任何通知送出給外部 LLM 或寫入紀錄前，
 * 強制將信用卡後四碼、完整卡號、CVV 與 OTP 驗證碼馬賽克遮蔽，降低盜刷風險。
 */
object CardRedactor {
    private val fullPanRegex = Regex("""\b(?:\d{4}[-\s]?){3,4}\d{1,4}\b""")
    private val last4Regexes = listOf(
        Regex("""(卡號末四碼|卡号末四码|末四碼|末四码|末4碼|末4码|尾號|尾号|後四碼|后四码|後4碼|后4码)(\s*[:：為是]?\s*)(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""(ending\s+in|card\s+ending|last\s+4)(\s*[:：]?\s*)(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""(\*{2,8}|x{2,8})(\s*)(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""(卡號|卡号|信用卡)(\s*[:：]?\s*)(\d{4})(?!\d)""", RegexOption.IGNORE_CASE),
    )
    private val parenCardRegex = Regex("""(卡|Visa|Mastercard|JCB)(\s*[（(])(\d{4})([）)])""", RegexOption.IGNORE_CASE)
    private val otpRegex = Regex("""(安全碼|驗證碼|動態密碼|OTP|CVV|CVC)(\s*[:：為是]?\s*)(\d{3,8})""", RegexOption.IGNORE_CASE)

    fun redact(raw: String): RedactionResult {
        var text = raw
        var count = 0

        text = fullPanRegex.replace(text) {
            count++
            "[卡號已遮蔽]"
        }

        for (regex in last4Regexes) {
            text = regex.replace(text) { m ->
                count++
                "${m.groupValues[1]}${m.groupValues[2]}****"
            }
        }

        text = parenCardRegex.replace(text) { m ->
            count++
            "${m.groupValues[1]}${m.groupValues[2]}****${m.groupValues[4]}"
        }

        text = otpRegex.replace(text) { m ->
            count++
            "${m.groupValues[1]}${m.groupValues[2]}${"*".repeat(m.groupValues[3].length)}"
        }

        return RedactionResult(redactedText = text, maskedCount = count)
    }
}
