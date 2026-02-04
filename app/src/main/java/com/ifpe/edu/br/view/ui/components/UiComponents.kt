/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
package com.ifpe.edu.br.view.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.model.repository.model.ChartType
import com.ifpe.edu.br.model.repository.remote.dto.agg.TelemetryKey
import com.ifpe.edu.br.model.repository.remote.dto.agg.TimeInterval
import com.ifpe.edu.br.view.ui.theme.tb_primary_light

private val intervalLabels = mapOf(
    TimeInterval.DAY to "Hoje",
    TimeInterval.WEEK to "Esta Semana",
    TimeInterval.MONTH to "Este Mês",
    TimeInterval.YEAR to "Este Ano"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeIntervalSelector(
    selectedInterval: TimeInterval,
    onIntervalSelected: (TimeInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = intervalLabels[selectedInterval] ?: selectedInterval.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Período de Consumo", color = tb_primary_light) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = filterSelectorColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TimeInterval.entries.forEach { interval ->
                    DropdownMenuItem(
                        text = {
                            Text(text = intervalLabels[interval] ?: interval.name)
                        },
                        onClick = {
                            onIntervalSelected(interval)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

private val chartTypeLabels = mapOf(
    ChartType.BAR to "Gráfico de coluna",
    ChartType.LINE to "Gráfico de linha"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartTypeSelector(
    chartType: ChartType,
    onTypeSelected: (ChartType) -> Unit,
    modifier: Modifier = Modifier
)
{
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = chartTypeLabels[chartType] ?: chartType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Estilo do gráfico", color = tb_primary_light) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = filterSelectorColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ChartType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(text = chartTypeLabels[type] ?: type.name)
                        },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

private val telemetryDisplayNames = mapOf(
    TelemetryKey.POWER to "Potência",
    TelemetryKey.CURRENT to "Corrente",
    TelemetryKey.VOLTAGE to "Tensão",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeySelector(
    telemetryKey: TelemetryKey,
    onTelemetryKeyChange: (TelemetryKey) -> Unit,
    modifier: Modifier = Modifier
)
{
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = telemetryDisplayNames[telemetryKey] ?: telemetryKey.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de dado", color = tb_primary_light) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = filterSelectorColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TelemetryKey.entries.forEach { key ->
                    DropdownMenuItem(
                        text = {
                            Text(text = telemetryDisplayNames[key] ?: key.name)
                        },
                        onClick = {
                            onTelemetryKeyChange(key)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
fun CustomFullScreenGradientBackground(
    modifier: Modifier = Modifier.fillMaxSize(),
    listColor: List<Color>,
) {
    Canvas(
        modifier = modifier
    ) {
        scale(scaleX = 1f, scaleY = 1f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listColor,
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width
                )
            )
        }
    }
}

@Composable
private fun filterSelectorColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
    unfocusedTextColor = tb_primary_light,
    focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
    focusedContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer
)