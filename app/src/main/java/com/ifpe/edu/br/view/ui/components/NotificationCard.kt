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
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifpe.edu.br.common.components.CustomCard
import com.ifpe.edu.br.common.components.CustomText
import com.ifpe.edu.br.common.ui.theme.cardCornerRadius
import com.ifpe.edu.br.model.repository.remote.dto.AirPowerNotificationItem
import com.ifpe.edu.br.view.ui.theme.app_default_solid_background_light
import com.ifpe.edu.br.view.ui.theme.tb_primary_light
import com.ifpe.edu.br.view.ui.theme.tb_secondary_light
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
@Composable
fun NotificationCard(
    item: AirPowerNotificationItem,
    onClick: () -> Unit
) {
    CustomCard(
        modifier = Modifier
            .clip(RoundedCornerShape(cardCornerRadius))
            .wrapContentHeight()
            .fillMaxWidth()
            .background(getCardColor(item.status))
            .clickable { onClick() },
        layouts = listOf {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(all = 0.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    CustomText(
                        color = tb_secondary_light,
                        alignment = TextAlign.Left,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        text = "assunto:"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(all = 0.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    GetPaddingStart()
                    CustomText(
                        color = tb_primary_light,
                        alignment = TextAlign.Left,
                        fontSize = 22.sp,
                        text = item.subject
                    )
                }
                GetDivider()
                GetPaddingStart()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    CustomText(
                        color = tb_secondary_light,
                        alignment = TextAlign.Left,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        text = "Origem:"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    GetPaddingStart()
                    CustomText(
                        color = tb_primary_light,
                        alignment = TextAlign.Left,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        text = item.alarmOriginatorName
                    )
                }
                GetDivider()
                GetPaddingStart()
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Column {
                        CustomText(
                            color = tb_secondary_light,
                            alignment = TextAlign.Left,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            text = "Severidade:"
                        )
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            GetPaddingStart()
                            CustomText(
                                color = tb_primary_light,
                                fontSize = 22.sp,
                                alignment = TextAlign.Right,
                                fontWeight = FontWeight.Normal,
                                text = item.alarmSeverity
                            )
                        }
                    }
                    Column {
                        CustomText(
                            color = tb_secondary_light,
                            alignment = TextAlign.Left,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            text = "Tipo:",
                            modifier = Modifier.padding(vertical = 0.dp)
                        )
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            GetPaddingStart()
                            CustomText(
                                color = tb_primary_light,
                                fontSize = 22.sp,
                                alignment = TextAlign.Right,
                                fontWeight = FontWeight.Normal,
                                text = item.alarmType
                            )
                        }
                    }
                }
                GetDivider()
                GetPaddingStart()
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column {
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CustomText(
                                color = tb_secondary_light,
                                alignment = TextAlign.Left,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                text = "Horário:"
                            )
                        }
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CustomText(
                                color = tb_primary_light,
                                fontSize = 20.sp,
                                alignment = TextAlign.Right,
                                fontWeight = FontWeight.Thin,
                                text = formatTimestamp(item.createdTime)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun GetDivider() {
    Divider(
        color = Color.Gray.copy(alpha = 0.3f),
        thickness = 1.dp
    )
}

@Composable
private fun GetPaddingStart(){
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun getCardColor(isNew: String): Color {
    return if (isNew == "SENT") {
        Color.White
    } else {
        app_default_solid_background_light
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MM/yy HH:mm", Locale.getDefault())
    return formatter.format(date)
}