package com.tangem.core.navigation.impl.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DefaultSystemNotificationsStateProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : SystemNotificationsStateProvider {

    override fun areNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}