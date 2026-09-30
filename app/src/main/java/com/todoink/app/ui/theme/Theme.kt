package com.todoink.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightPrimary,
    secondaryContainer = LightPrimaryContainer,
    onSecondaryContainer = LightOnPrimaryContainer,
    background = PaperLightBackground,
    onBackground = InkLightOnSurface,
    surface = PaperLightSurface,
    onSurface = InkLightOnSurface,
    surfaceVariant = LightPrimaryContainer,
    onSurfaceVariant = LightSecondaryText,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkPrimary,
    secondaryContainer = DarkPrimaryContainer,
    onSecondaryContainer = DarkOnPrimaryContainer,
    background = PaperDarkBackground,
    onBackground = InkDarkOnSurface,
    surface = PaperDarkSurface,
    onSurface = InkDarkOnSurface,
    surfaceVariant = DarkPrimaryContainer,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

/** 设计稿 §5：品牌强调色固定紫色，不随系统动态取色。 */
val LocalTodoInkExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** 设计稿 §7：页面读取扩展语义色的统一入口，不在页面散落硬编码颜色。 */
@Composable
fun todoInkExtendedColors(): TodoInkExtendedColors = LocalTodoInkExtendedColors.current

@Composable
fun TodoInkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalTodoInkExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = TodoInkShapes,
            content = content,
        )
    }
}
