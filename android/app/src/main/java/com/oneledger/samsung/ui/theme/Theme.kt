package com.oneledger.samsung.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SamsungBlue = Color(0xFF0381FE)
val SamsungBlueDark = Color(0xFF389BFF)
val IncomeGreen = Color(0xFF148A4B)
val IncomeGreenDark = Color(0xFF36C978)
val ExpenseCoral = Color(0xFFE05246)
val ExpenseCoralDark = Color(0xFFFF6B60)

private val OneUiDarkScheme = darkColorScheme(
    primary = SamsungBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF132C4C),
    onPrimaryContainer = SamsungBlueDark,
    background = Color(0xFF000000), // Samsung AMOLED True Black
    onBackground = Color(0xFFF5F6F8),
    surface = Color(0xFF17181C),
    onSurface = Color(0xFFF5F6F8),
    surfaceVariant = Color(0xFF22242A),
    onSurfaceVariant = Color(0xFF989FA9),
    outline = Color(0xFF32353E),
    error = ExpenseCoralDark,
)

private val OneUiLightScheme = lightColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F0FF),
    onPrimaryContainer = SamsungBlue,
    background = Color(0xFFF2F3F5),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFF5F6F8),
    onSurfaceVariant = Color(0xFF606773),
    outline = Color(0xFFE2E5EB),
    error = ExpenseCoral,
)

@Composable
fun OneLedgerTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) OneUiDarkScheme else OneUiLightScheme,
        content = content,
    )
}
