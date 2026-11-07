package de.wissel.info.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import de.wissel.info.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "wissel_waste_channel"
    private const val CHANNEL_NAME = "Müllabfuhr Wissel"

    private const val CHANNEL_ID_POLITICS = "wissel_politics_channel"
    private const val CHANNEL_NAME_POLITICS = "Lokalpolitik & Rat Kalkar"

    private const val CHANNEL_ID_ALERTS = "wissel_severe_alerts"
    private const val CHANNEL_NAME_ALERTS = "Amtliche Warnungen & Katastrophenschutz"

    private const val CHANNEL_ID_PEGEL = "wissel_pegel_channel"
    private const val CHANNEL_NAME_PEGEL = "Rheinpegel & Hochwasserschutz" 

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val wasteChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Erinnerungen an die Müllabfuhr in Wissel am Vorabend"
                enableVibration(true)
            }
            manager.createNotificationChannel(wasteChannel)

            val politicsChannel = NotificationChannel(
                CHANNEL_ID_POLITICS,
                CHANNEL_NAME_POLITICS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ankündigungen von Ratssitzungen und neue Beschlüsse mit Wissel-Bezug"
                enableVibration(true)
            }
                        manager.createNotificationChannel(politicsChannel)

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "DWD-Unwetterwarnungen und amtliche MoWaS-Gefahrendurchsagen"
                enableVibration(true)
            }
            manager.createNotificationChannel(alertsChannel)

            val pegelChannel = NotificationChannel(
                CHANNEL_ID_PEGEL,
                CHANNEL_NAME_PEGEL,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hochwassermarken und Schwellenwert-Meldungen für den Rheinpegel Rees"
                enableVibration(true)
            }
            manager.createNotificationChannel(pegelChannel)
        }
    }

    fun sendWasteReminder(
        context: Context,
        title: String = "Müllabfuhr Wissel: Morgen Leerung!",
        message: String = "Morgen werden Leichtverpackungen & Restabfall geleert. Bitte bis 06:00 Uhr rausstellen."
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, builder.build())
    }

    fun sendCouncilMeetingReminder(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 2001
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_tab", "politics")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_POLITICS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun sendCouncilDecisionAlert(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 2002
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_tab", "politics")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_POLITICS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun sendSevereAlert(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 4001
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun sendPegelAlert(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 5001
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PEGEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
