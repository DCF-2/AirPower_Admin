package com.ifpe.edu.br.view.manager

/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ifpe.edu.br.R
import com.ifpe.edu.br.model.repository.remote.dto.AirPowerNotificationItem
import kotlin.random.Random

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "airpower_notifications_channel"
        private const val CHANNEL_NAME = "AirPower Notifications"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for AirPower app notifications"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(notificationItem: AirPowerNotificationItem) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.airpower_icon)
            .setContentTitle(notificationItem.subject)
            .setContentText(notificationItem.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Random.nextInt(), notification)
    }
}