package com.ifpe.edu.br.view.ui.theme
/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
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
val surfaceDarkAppColor = Color(0xE21F1F1F)
val onSurfaceDarkAppColor = Color(0xFFD7DCEA)

// -_------------------------_------------------------_--------_
val tb_primary_light = Color(0xFF305680)
val tb_secondary_light = Color(0xFFFF5722)
val tb_tertiary_light = Color(0xFFEEEEEE)
val app_default_solid_background_light = Color(0xD7DAD6D6)
val app_default_solid_background_dark = Color(0xD79A9696)
val app_default_solid_background_dark_variant = Color(0xD7C4C0C0)

val a = Color(0xE9FAF9F9)
val b = Color(0xD7C2BEBE)

private val transparentGradient = listOf(
    a, b
)

val appBackgroundGradientLight = listOf(
    Color.White, app_default_solid_background_light
)

val appBackgroundGradientDark = listOf(
    app_default_solid_background_dark_variant, app_default_solid_background_dark
)

@Composable
fun DefaultTransparentGradient(
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(
        modifier = modifier
    ) {
        scale(scaleX = 1f, scaleY = 1f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = transparentGradient,
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width * 0.8f
                )
            )
        }
    }
}

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