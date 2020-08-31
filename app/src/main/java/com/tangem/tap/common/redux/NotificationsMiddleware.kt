package com.tangem.tap.common.redux

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tangem.TangemError
import com.tangem.tap.notificationsHandler
import org.rekotlin.Action
import org.rekotlin.Middleware
import java.lang.ref.WeakReference

class NotificationsHandler(coordinatorLayout: CoordinatorLayout) {
    private val coordinatorLayoutWeak = WeakReference(coordinatorLayout)

    fun showNotification(message: String) {
        coordinatorLayoutWeak.get()?.let { layout ->
            Snackbar.make(layout, message, Snackbar.LENGTH_LONG)
                .also { snackbar -> snackbar.show() }
        }
    }

    fun showNotification(message: Int) {
        coordinatorLayoutWeak.get()?.let {
            showNotification(it.context.getString(message))
        }
    }
}

val notificationsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            if (action is NotificationAction) {
                notificationsHandler?.showNotification(action.messageResource)
            }
            if (action is ErrorAction) {
                notificationsHandler?.showNotification(action.error.customMessage)
            }
            next(action)
        }
    }
}

interface NotificationAction : Action {
    val messageResource: Int
}

interface ErrorAction : Action {
    val error: TangemError
}