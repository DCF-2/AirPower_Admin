package com.ifpe.edu.br.view.ui.theme
/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/

import androidx.compose.ui.graphics.Color
import com.ifpe.edu.br.common.contracts.AppColorScheme

// Light Colors
val primaryLightAppColor = Color(0xFF25669B)
val onPrimaryLightAppColor = Color(0xFAD8DADE)
val secondaryLightAppColor = Color(0xFFFF5722)
val onSecondaryLightAppColor = Color(0xFFEAE3E1)
val tertiaryLightAppColor = Color(0xFFFFFFFF)
val onTertiaryLightAppColor = Color(0xFF212020)
val backgroundLightAppColor = Color(0xFFF3F1F1)
val onBackgroundLightAppColor = Color(0xFF636465)
val surfaceLightAppColor = Color(0xF3E8E7E7)
val onSurfaceLightAppColor = Color(0xFF42586B)
val secondaryContainerLightAppColor = Color(0xFFFF5722)
val onSecondaryContainerLightAppColor = Color(0xFFFF5722)


// Dark Colors

val primaryDarkAppColor = Color(0xFF3E608A)
val onPrimaryDarkAppColor = Color(0xFFC5C7CB)
val secondaryDarkAppColor = Color(0xFFFF5722)
val onSecondaryDarkAppColor = Color(0xFFDCD4D2)
val secondaryContainerDarkAppColor = Color(0xFFFF5722)
val onSecondaryContainerDarkAppColor = Color(0xFFFF5722)

val tertiaryDarkAppColor = Color(0xFFFF5722)
val onTertiaryDarkAppColor = Color(0xFFFF5722)
val backgroundDarkAppColor = Color(0xFF313136)
val onBackgroundDarkAppColor = Color(0xFFCBD0D5)
val surfaceDarkAppColor = Color(0xE91F1F1F)
val onSurfaceDarkAppColor = Color(0xFFD7DCEA)

val lightAppThemeSchema = AppColorScheme(
    primary = primaryLightAppColor,
    onPrimary = onPrimaryLightAppColor,

    secondary = secondaryLightAppColor,
    onSecondary = onSecondaryLightAppColor,

    tertiary = tertiaryLightAppColor,
    onTertiary = onTertiaryLightAppColor,

    primaryContainer = primaryLightAppColor.copy(alpha = 0.1f),
    onPrimaryContainer = primaryLightAppColor,

    secondaryContainer = secondaryContainerLightAppColor.copy(alpha = 0.15f),
    onSecondaryContainer = onSecondaryContainerLightAppColor,

    background = backgroundLightAppColor,
    onBackground = onBackgroundLightAppColor,

    surface = surfaceLightAppColor,
    onSurface = onSurfaceLightAppColor
)

val darkAppThemeSchema = AppColorScheme(
    primary = primaryDarkAppColor,
    onPrimary = onPrimaryDarkAppColor,

    secondary = secondaryDarkAppColor,
    onSecondary = onSecondaryDarkAppColor,

    primaryContainer = primaryDarkAppColor.copy(alpha = 0.1f),
    onPrimaryContainer = primaryDarkAppColor,

    secondaryContainer = secondaryContainerDarkAppColor.copy(alpha = 0.15f),
    onSecondaryContainer = onSecondaryContainerDarkAppColor,

    tertiary = tertiaryDarkAppColor,
    onTertiary = onTertiaryDarkAppColor,

    background = backgroundDarkAppColor,
    onBackground = onBackgroundDarkAppColor,

    surface = surfaceDarkAppColor,
    onSurface = onSurfaceDarkAppColor
)