package com.coloniavictoria.municipal

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificacionHelper {

    private const val CHANNEL_ID = "reclamos"

    fun mostrar(
        context: Context,
        numero: String,
        estado: String
    ) {
        crearCanal(context)

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Actualización de reclamo")
            .setContentText("$numero: $estado")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(numero.hashCode(), notification)
    }

    private fun crearCanal(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Actualizaciones de reclamos",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(canal)
        }
    }
}
