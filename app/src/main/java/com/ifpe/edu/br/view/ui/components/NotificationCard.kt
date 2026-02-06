/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
package com.ifpe.edu.br.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.ui.theme.AirPowerTheme
import com.ifpe.edu.br.common.ui.theme.cardCornerRadius
import com.ifpe.edu.br.model.repository.remote.dto.AirPowerNotificationItem
import com.ifpe.edu.br.view.ui.screens.SimpleRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCard(
    item: AirPowerNotificationItem,
    onClick: () -> Unit
) {
    val dimens = AirPowerTheme.dimens

    CustomCard(
        modifier = Modifier
            .clip(RoundedCornerShape(cardCornerRadius))
            .wrapContentHeight()
            .fillMaxWidth()
            .background(getCardColor(item.status))
            .clickable { onClick() },
        paddingStart = dimens.paddingSmall,
        paddingEnd = dimens.paddingSmall / 3,
        paddingTop = dimens.paddingSmall / 3,
        paddingBottom = dimens.paddingMedium,
        layouts = listOf {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                PaddingSmall()
                SimpleRow(
                    layouts = listOf {
                        CustomText(
                            alignment = TextAlign.Left,
                            color = AirPowerTheme.color.onPrimaryContainer,
                            fontStyle = AirPowerTheme.typography.bodyLarge,
                            text = "Assunto:"
                        )
                    }
                )
                SimpleRow(
                    layouts = listOf {
                        GetPaddingStart()
                        CustomText(
                            fontStyle = AirPowerTheme.typography.bodyLarge,
                            color = AirPowerTheme.color.onSecondaryContainer,
                            alignment = TextAlign.Left,
                            text = item.subject
                        )
                    }
                )
                GradientDivider()
                PaddingSmall()

                SimpleRow(
                    layouts = listOf {
                        CustomText(
                            color = AirPowerTheme.color.onPrimaryContainer,
                            fontStyle = AirPowerTheme.typography.bodyLarge,
                            alignment = TextAlign.Left,
                            text = "Origem:"
                        )
                    }
                )
                SimpleRow(
                    layouts = listOf {
                        CustomText(
                            fontStyle = AirPowerTheme.typography.bodyLarge,
                            color = AirPowerTheme.color.onSecondaryContainer,
                            alignment = TextAlign.Left,
                            text = item.alarmOriginatorName
                        )
                    }
                )
                GradientDivider()
                PaddingSmall()

                SimpleRow(
                    layouts = listOf {
                        Column {
                            CustomText(
                                color = AirPowerTheme.color.onPrimaryContainer,
                                fontStyle = AirPowerTheme.typography.bodyLarge,
                                alignment = TextAlign.Left,
                                text = "Severidade:"
                            )
                            Row(
                                modifier = Modifier.wrapContentSize(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                GetPaddingStart()
                                CustomText(
                                    fontStyle = AirPowerTheme.typography.bodyLarge,
                                    color = AirPowerTheme.color.onSecondaryContainer,
                                    alignment = TextAlign.Right,
                                    text = item.alarmSeverity
                                )
                            }
                        }
                        Column {
                            CustomText(
                                color = AirPowerTheme.color.onPrimaryContainer,
                                fontStyle = AirPowerTheme.typography.bodyLarge,
                                alignment = TextAlign.Left,
                                text = "Tipo:",
                                modifier = Modifier.padding(vertical = 0.dp)
                            )
                            Row(
                                modifier = Modifier.wrapContentSize(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                GetPaddingStart()
                                CustomText(
                                    fontStyle = AirPowerTheme.typography.bodyLarge,
                                    color = AirPowerTheme.color.onSecondaryContainer,
                                    alignment = TextAlign.Right,
                                    text = item.alarmType
                                )
                            }
                        }
                    }
                )
                GradientDivider()
                PaddingSmall()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    Column {
                        CustomText(
                            color = AirPowerTheme.color.onPrimaryContainer,
                            fontStyle = AirPowerTheme.typography.bodySmall,
                            alignment = TextAlign.Start,
                            text = "Horário:"
                        )
                        CustomText(
                            fontStyle = AirPowerTheme.typography.bodySmall,
                            color = AirPowerTheme.color.onSecondaryContainer,
                            alignment = TextAlign.Right,
                            text = formatTimestamp(item.createdTime)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PaddingSmall() {
    Spacer(modifier = Modifier.padding(vertical = AirPowerTheme.dimens.paddingSmall))
}

@Composable
private fun GetPaddingStart() {
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun getCardColor(isNew: String): Color {
    return if (isNew == "SENT") {
        AirPowerTheme.color.secondaryContainer
    } else {
        AirPowerTheme.color.primaryContainer
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MM/yy HH:mm", Locale.getDefault())
    return formatter.format(date)
}