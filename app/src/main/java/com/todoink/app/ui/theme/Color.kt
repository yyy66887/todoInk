package com.todoink.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 设计稿 §5 token：纸白 / 墨黑 / 紫。浅色
val PaperLightBackground = Color(0xFFF7F3EE)
val PaperLightSurface = Color(0xFFFFFCF8)
val InkLightOnSurface = Color(0xFF1C1B1F)
val LightSecondaryText = Color(0xFF69636F)
val LightPrimary = Color(0xFF6750A4)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEEE7F7)
val LightOnPrimaryContainer = Color(0xFF241544)
val LightOutline = Color(0xFFDAD3DF)
val LightError = Color(0xFFA83735)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFCE9E6)
val LightOnErrorContainer = Color(0xFF41201F)
val LightWarning = Color(0xFF875900)
val LightWarningContainer = Color(0xFFFFF1D6)
val LightOnWarningContainer = Color(0xFF2C1D00)
val LightSuccess = Color(0xFF35624F)
val LightSuccessContainer = Color(0xFFE5F0E9)
val LightOnSuccessContainer = Color(0xFF0E2318)
val LightMuted = Color(0xFFEFEBE6)
val LightQuote = Color(0xFFF0ECE6)

// 设计稿 §5 token：深色
val PaperDarkBackground = Color(0xFF181719)
val PaperDarkSurface = Color(0xFF242226)
val InkDarkOnSurface = Color(0xFFF1EDF3)
val DarkSecondaryText = Color(0xFFC1BAC6)
val DarkPrimary = Color(0xFFD0BCFF)
val DarkOnPrimary = Color(0xFF241544)
val DarkPrimaryContainer = Color(0xFF3A2C52)
val DarkOnPrimaryContainer = Color(0xFFEEE7F7)
val DarkOutline = Color(0xFF4D4654)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF41201F)
val DarkErrorContainer = Color(0xFF492522)
val DarkOnErrorContainer = Color(0xFFFCE9E6)
val DarkWarning = Color(0xFFF4CC80)
val DarkWarningContainer = Color(0xFF3C2F18)
val DarkOnWarningContainer = Color(0xFFFFF1D6)
val DarkSuccess = Color(0xFFA4D3BA)
val DarkSuccessContainer = Color(0xFF22392D)
val DarkOnSuccessContainer = Color(0xFFE5F0E9)
val DarkMuted = Color(0xFF302C32)
val DarkQuote = Color(0xFF302C32)

/** 设计稿 §5 的扩展语义色：模糊时间 / 待处理异常（warning）与实际完成 / 实际连接（success）。 */
@Immutable
data class TodoInkExtendedColors(
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color,
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val muted: Color,
    val quote: Color,
)

val LightExtendedColors = TodoInkExtendedColors(
    warning = LightWarning,
    onWarningContainer = LightOnWarningContainer,
    warningContainer = LightWarningContainer,
    success = LightSuccess,
    onSuccessContainer = LightOnSuccessContainer,
    successContainer = LightSuccessContainer,
    muted = LightMuted,
    quote = LightQuote,
)

val DarkExtendedColors = TodoInkExtendedColors(
    warning = DarkWarning,
    onWarningContainer = DarkOnWarningContainer,
    warningContainer = DarkWarningContainer,
    success = DarkSuccess,
    onSuccessContainer = DarkOnSuccessContainer,
    successContainer = DarkSuccessContainer,
    muted = DarkMuted,
    quote = DarkQuote,
)
