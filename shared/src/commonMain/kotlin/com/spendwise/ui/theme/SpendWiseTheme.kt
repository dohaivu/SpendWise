package com.spendwise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.spendwise.ui.AppColorSchemeMode
import com.spendwise.ui.AppThemeMode

private val SpendWiseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SpendWiseTheme(
    themeMode: AppThemeMode = AppThemeMode.System,
    colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.SkyBlue,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    },
    colorScheme: ColorScheme = when (colorSchemeMode) {
        AppColorSchemeMode.Sunset -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
        AppColorSchemeMode.SkyBlue -> if (darkTheme) SkyBlueDarkColorScheme else SkyBlueLightColorScheme
        AppColorSchemeMode.SystemDefault -> if (darkTheme) darkColorScheme() else lightColorScheme()
    },
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = SpendWiseShapes,
        content = content
    )
}
