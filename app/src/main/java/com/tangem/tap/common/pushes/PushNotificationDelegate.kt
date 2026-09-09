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
import coil.imageLoader
import coil.request.ImageRequest
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.tap.MainActivity
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.notifications.NotificationIdGenerator
import com.tangem.wallet.R

class PushNotificationDelegate(
    private val context: Context,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) {

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
        val notificationId = NotificationIdGenerator.next()

        val intent = Intent(context, MainActivity::class.java).apply {
            dataMap.forEach { (key, value) ->
                putExtra(key, value)
            }
            putExtra(OPENED_FROM_GCM_PUSH, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ notificationId,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
                if (imageUrl != null) {
                    val bitmap = getBitmapImageFromUrl(imageUrl)
                    setStyle(
                        NotificationCompat
                            .BigPictureStyle()
                            .bigPicture(bitmap),
                    ).setLargeIcon(bitmap)
                }
            }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager == null) {
            reportFailure(IllegalStateException("NotificationManager is not available"))
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    channelId,
                    ContextCompat.getString(context, R.string.tangem_app_name),
                    NotificationManager.IMPORTANCE_HIGH,
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }

            notificationManager.notify(
                /* id = */ notificationId,
                /* notification = */ notificationBuilder.build(),
            )
        } catch (e: SecurityException) {
            reportFailure(e)
        }
    }

    /**
     * The push is lost for the user either way, so the failure is reported as a non-fatal — otherwise the guard
     * silently hides how many users never receive notifications.
     */
    private fun reportFailure(e: Throwable) {
        TangemLogger.e("Failed to post push notification", e)
        analyticsExceptionHandler.sendException(ExceptionAnalyticsEvent(exception = e))
    }

    private fun getBitmapImageFromUrl(url: Uri): Bitmap? {
        return context.imageLoader.executeBlocking(
            ImageRequest.Builder(context)
                .data(url)
                .build(),
        ).drawable?.toBitmap()
    }

    private companion object {
        private const val OPENED_FROM_GCM_PUSH = "google.sent_time" // every bundle from FCM contains this key
    }
}