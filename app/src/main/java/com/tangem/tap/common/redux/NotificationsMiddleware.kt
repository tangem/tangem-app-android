package com.tangem.tap.common.redux

import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.domain.MultiMessageError
import com.tangem.tap.domain.TapArgError
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

    fun showNotification(message: Int, args: List<Any>? = null) {
        baseLayout.get()?.let {
            showNotification(getMessageString(message, args))
        }
    }

    fun showToastNotification(message: Int, args: List<Any>? = null) {
        baseLayout.get()?.let {
            Toast.makeText(it.context, getMessageString(message, args), Toast.LENGTH_LONG).show()
        }
    }

    fun showNotification(errorList: List<Int>, builder: (List<String>) -> String) {
        val context = baseLayout.get()?.context ?: return

        val message = builder(errorList.map { context.getString(it) })
        showNotification(message)
    }

    private fun getMessageString(message: Int, args: List<Any>?): String {
        val context = baseLayout.get()?.context ?: return ""

        return if (args.isNullOrEmpty()) {
            context.getString(message)
        } else {
            context.getString(message, *args.toTypedArray())
        }
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
                        else -> {
                            val args = (action.error as? TapArgError)?.args ?: listOf()
                            notificationsHandler?.showNotification(action.error.localizedMessage, args)
                        }
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