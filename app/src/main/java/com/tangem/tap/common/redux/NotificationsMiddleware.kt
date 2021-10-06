package com.tangem.tap.common.redux

import android.content.Context
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.domain.ArgError
import com.tangem.tap.domain.MultiMessageError
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.assembleErrors
import com.tangem.tap.notificationsHandler
import com.tangem.wallet.BuildConfig
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
            showNotification(getMessageString(it.context, message, args))
        }
    }

    fun showToastNotification(message: Int, args: List<Any>? = null) {
        baseLayout.get()?.let {
            Toast.makeText(it.context, getMessageString(it.context, message, args), Toast.LENGTH_LONG).show()
        }
    }

    fun showNotification(errorList: List<Pair<Int, List<Any>?>>, builder: (List<String>) -> String) {
        val context = baseLayout.get()?.context ?: return

        val message = builder(errorList.map { getMessageString(context, it.first, it.second) })
        showNotification(message)
    }
}

fun getMessageString(context: Context, message: Int, args: List<Any>?): String {
    return if (args.isNullOrEmpty()) {
        context.getString(message)
    } else {
        context.getString(message, *args.toTypedArray())
    }
}

val notificationsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleNotificationAction(action)
            next(action)
        }
    }
}

private fun handleNotificationAction(action: Action) {
    if (action is Debug && !BuildConfig.DEBUG) return

    when (action) {
        is NotificationAction -> notificationsHandler?.showNotification(action.messageResource)
        is ToastNotificationAction -> notificationsHandler?.showToastNotification(action.messageResource)
        is ErrorAction -> {
            when (action.error) {
                is MultiMessageError -> {
                    val multiError = action.error as MultiMessageError
                    notificationsHandler?.showNotification(multiError.assembleErrors(), multiError.builder)
                }
                else -> {
                    val args = (action.error as? ArgError)?.args ?: listOf()
                    notificationsHandler?.showNotification(action.error.messageResource, args)
                }
            }
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

// Processed only in the debug builds
interface Debug
interface DebugNotification : Debug, NotificationAction
interface DebugToastNotification : Debug, ToastNotificationAction
interface DebugErrorAction : Debug, ErrorAction