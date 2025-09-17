package com.tangem.tap.common.pushes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.executeBlocking
import coil.request.ImageRequest
import com.tangem.domain.common.LogConfig
import com.tangem.tap.MainActivity
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.wallet.R

class PushNotificationDelegate(private val context: Context) {

    @Suppress("LongParameterList")
    fun showNotification(
        dataMap: Map<String, String>,
        title: String?,
        body: String?,
        channelId: String,
        priority: Int,
        imageUrl: Uri? = null,
        vibratePattern: LongArray?,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            dataMap.forEach { (key, value) ->
                putExtra(key, value)
            }
            putExtra(OPENED_FROM_GCM_PUSH, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ PUSH_NOTIFICATION_REQUEST_CODE,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tangem_24)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(vibratePattern)
            .apply {
                imageUrl?.let { uri ->
                    val bitmap = getBitmapImageFromUrl(uri)
                    setStyle(
                        NotificationCompat
                            .BigPictureStyle()
                            .bigPicture(bitmap),
                    ).setLargeIcon(bitmap)
                }
            }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                ContextCompat.getString(context, R.string.tangem_app_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Generating unique notification id
        val uniqueId = (System.currentTimeMillis() % Integer.MAX_VALUE).toInt()

        notificationManager.notify(
            /* id = */ uniqueId,
            /* notification = */ notificationBuilder.build(),
        )
    }

    private fun getBitmapImageFromUrl(url: Uri): Bitmap? {
        return createCoilImageLoader(
            context,
            logEnabled = LogConfig.imageLoader,
        ).executeBlocking(
            ImageRequest.Builder(context)
                .data(url)
                .build(),
        ).drawable?.toBitmap()
    }

    private companion object {
        const val PUSH_NOTIFICATION_REQUEST_CODE = 123
        private const val OPENED_FROM_GCM_PUSH = "google.sent_time" // every bundle from FCM contains this key
    }
}