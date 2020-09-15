package com.tangem.tap.common.redux

import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.domain.TapError
import com.tangem.tap.notificationsHandler
import org.rekotlin.Action
import org.rekotlin.Middleware
import java.lang.ref.WeakReference

class NotificationsHandler(coordinatorLayout: CoordinatorLayout) {
    private val basicCoordinatorLayout = WeakReference(coordinatorLayout)
    private var baseLayout = basicCoordinatorLayout

    fun replaceBaseLayout(coordinatorLayout: CoordinatorLayout) {
        baseLayout = WeakReference(coordinatorLayout)
    }

    fun returnBaseLayout() {
        baseLayout = basicCoordinatorLayout
    }

    fun showNotification(message: String) {
        baseLayout.get()?.let { layout ->
            Snackbar.make(layout, message, Snackbar.LENGTH_LONG)
                    .also { snackbar -> snackbar.show() }
        }
    }

    fun showNotification(message: Int) {
        baseLayout.get()?.let {
            showNotification(it.context.getString(message))
        }
    }

    fun showToastNotification(message: Int) {
        baseLayout.get()?.let {
            Toast.makeText(it.context, it.context.getString(message), Toast.LENGTH_LONG).show()
        }
    }
}

val notificationsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            if (action is NotificationAction) {
                notificationsHandler?.showNotification(action.messageResource)
            }
            if (action is ToastNotificationAction) {
                notificationsHandler?.showToastNotification(action.messageResource)
            }
            if (action is ErrorAction) {
                notificationsHandler?.showNotification(action.error.localizedMessage)
            }
            next(action)
        }
    }
}

interface ToastNotificationAction : Action {
    val messageResource: Int
}

interface NotificationAction : Action {
    val messageResource: Int
}

interface ErrorAction : Action {
    val error: TapError
}