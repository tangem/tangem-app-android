package com.tangem.tap.common.redux

import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.domain.MultiMessageError
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.assembleErrorIds
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

    fun showNotification(errorList: List<Int>, builder: (List<String>) -> String) {
        val context = baseLayout.get()?.context ?: return

        val message = builder(errorList.map { context.getString(it) })
        showNotification(message)
    }
}

val notificationsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is NotificationAction -> notificationsHandler?.showNotification(action.messageResource)
                is ToastNotificationAction -> notificationsHandler?.showToastNotification(action.messageResource)
                is ErrorAction -> {
                    when (action.error) {
                        is MultiMessageError -> {
                            val multiError = action.error as MultiMessageError
                            notificationsHandler?.showNotification(multiError.assembleErrorIds(), multiError.builder)
                        }
                        else -> notificationsHandler?.showNotification(action.error.localizedMessage)
                    }
                }
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