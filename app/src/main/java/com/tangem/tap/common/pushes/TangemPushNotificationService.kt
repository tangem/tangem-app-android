package com.tangem.tap.common.pushes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.executeBlocking
import coil.request.ImageRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tangem.domain.common.LogConfig
import com.tangem.tap.MainActivity
import com.tangem.tap.common.images.createCoilImageLoader
import com.tangem.tap.features.intentHandler.handlers.OnPushClickedIntentHandler
import com.tangem.wallet.R
import timber.log.Timber

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class TangemPushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token received: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification ?: return
        val channelId = notification.channelId ?: TANGEM_CHANNEL_ID

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            message.data.forEach {
                putExtra(it.key, it.value)
            }
            putExtra(OnPushClickedIntentHandler.OPENED_FROM_GCM_PUSH, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            /* context = */ this,
            /* requestCode = */ PUSH_NOTIFICATION_REQUEST_CODE,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_tangem_24)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setPriority(message.priority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(notification.vibrateTimings)
                .apply {
                    notification.imageUrl?.let { uri ->
                        val bitmap = getBitmapImageFromUrl(uri)
                        setStyle(
                            NotificationCompat
                                .BigPictureStyle()
                                .bigPicture(bitmap),
                        ).setLargeIcon(bitmap)
                    }
                }

        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                ContextCompat.getString(applicationContext, R.string.tangem_app_name),
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
            applicationContext,
            logEnabled = LogConfig.imageLoader,
        ).executeBlocking(
            ImageRequest.Builder(applicationContext)
                .data(url)
                .build(),
        ).drawable?.toBitmap()
    }

    private companion object {
        const val TANGEM_CHANNEL_ID = "Tangem General" // General channel for notifications
        const val PUSH_NOTIFICATION_REQUEST_CODE = 123
    }
}