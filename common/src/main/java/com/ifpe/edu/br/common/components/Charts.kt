// Trabalho de conclusão de curso - IFPE 2026
// Author: Willian Santos
// Project: AirPower Costumer

// Copyright (c) 2025 IFPE. All rights reserved.package com.ifpe.edu.br.common.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.contracts.ChartDataWrapper
import com.ifpe.edu.br.common.ui.theme.ColorPrimaryLight
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.GridProperties.AxisProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.Line

@Composable
fun CustomBarChart(
    paddingStart: Dp = 0.dp,
    paddingTop: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    height: Dp = 250.dp,
    thickNes: Dp = 10.dp,
    spacing: Dp = 5.dp,
    dataWrapper: ChartDataWrapper
) {
    val properties = BarProperties(thickNes, spacing)

    ColumnChart(
        modifier = Modifier
            .height(height)
            .padding(
                start = paddingStart,
                top = paddingTop,
                end = paddingEnd,
                bottom = paddingBottom
            ),
        barProperties = properties,
        data = remember {
            dataWrapper.getDataSet().toBar()
        },
        animationSpec = tween(durationMillis = 300),
        animationDelay = 100,
        animationMode = AnimationMode.Together { 100L },
        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(fontSize = 10.sp),
            count = IndicatorCount.CountBased(count = 10)
        ),
        gridProperties = GridProperties(
            xAxisProperties = AxisProperties(
                enabled = true,
                lineCount = 10,
            ),
            yAxisProperties = AxisProperties(
                enabled = true,
                lineCount = 5,
            )
        )
    )
}

@Composable
fun CustomLineChart(
    paddingStart: Dp = 0.dp,
    paddingTop: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    height: Dp = 250.dp,
    dataWrapper: ChartDataWrapper
) {
    val chartValues = remember(dataWrapper.getDataSet()) {
        dataWrapper.getDataSet().data.map { it.verticalValue }
    }

    val lineData = remember(chartValues) {
        listOf(
            Line(
                label = dataWrapper.getName(),
                values = chartValues,
                color = SolidColor(ColorPrimaryLight),
                firstGradientFillColor = ColorPrimaryLight.copy(alpha = 0.5f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(1000),
                gradientAnimationDelay = 500,
                drawStyle = DrawStyle.Stroke(width = 3.dp),
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(ColorPrimaryLight),
                    strokeWidth = 2.dp,
                    radius = 3.dp
                )
            )
        )
    }

    LineChart(
        modifier = Modifier
            .height(height)
            .padding(
                start = paddingStart,
                top = paddingTop,
                end = paddingEnd,
                bottom = paddingBottom
            ),
        data = lineData,
        animationMode = AnimationMode.Together { it * 100L },
        gridProperties = GridProperties(
            xAxisProperties = AxisProperties(
                enabled = true,
                lineCount = chartValues.size.coerceAtMost(10),
            ),
            yAxisProperties = AxisProperties(
                enabled = true,
                lineCount = 5,
            )
        ),

        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(fontSize = 10.sp, color = ColorPrimaryLight),
            count = IndicatorCount.CountBased(count = 5)
        )
    )
}
