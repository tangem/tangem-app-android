package com.tangem.core.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.res.LocalIsInDarkTheme

@Composable
fun DialogFullScreen(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = true, // must be true as a part of work around
            decorFitsSystemWindows = false,
        ),
        content = {
            ProvideSystemBarsIconsController {
                val activityWindow = getActivityWindow()
                val dialogWindow = getDialogWindow()
                val parentView = LocalView.current.parent as View
                SideEffect {
                    if (activityWindow != null && dialogWindow != null) {
                        val attributes = WindowManager.LayoutParams().apply {
                            copyFrom(activityWindow.attributes)
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            } else {
                                flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            }
                            type = dialogWindow.attributes.type
                        }

                        dialogWindow.attributes = attributes
                        parentView.layoutParams =
                            FrameLayout.LayoutParams(
                                activityWindow.decorView.width,
                                activityWindow.decorView.height,
                            )
                    }
                }

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    val systemUiController = rememberSystemUiController(getActivityWindow())
                    val dialogSystemUiController = rememberSystemUiController(getDialogWindow())

                    SideEffect {
                        systemUiController.setSystemBarsColor(color = Color.Transparent)
                        dialogSystemUiController.setSystemBarsColor(color = Color.Transparent)
                    }
                }

                SystemBarsIconsDisposable(darkIcons = LocalIsInDarkTheme.current.not())

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    content()
                }
            }
        },
    )
}

// Window utils
@Composable
private fun getDialogWindow(): Window? = (LocalView.current.parent as? DialogWindowProvider)?.window

@Composable
private fun getActivityWindow(): Window? = LocalView.current.context.getActivityWindow()

private tailrec fun Context.getActivityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.getActivityWindow()
    else -> null
}