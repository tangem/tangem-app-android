package com.tangem.core.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID

@Composable
fun FullScreen(
    notTouchable: Boolean = false,
    focusable: Boolean = false,
    onBackClick: () -> Unit = {},
    content: @Composable (() -> Unit) -> Unit,
) {
    val view = LocalView.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val id = rememberSaveable { UUID.randomUUID() }

    val fullScreenLayout = remember {
        FullScreenLayout(
            notTouchable = notTouchable,
            focusable = focusable,
            composeView = view,
            onBackClick = onBackClick,
            uniqueId = id,
        ).apply {
            setContent(parentComposition) {
                currentContent(::dismiss)
            }
        }
    }

    DisposableEffect(fullScreenLayout) {
        fullScreenLayout.show()
        onDispose { fullScreenLayout.dispose() }
    }
}

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
private class FullScreenLayout(
    private val notTouchable: Boolean,
    private val focusable: Boolean,
    private val composeView: View,
    private val onBackClick: () -> Unit,
    uniqueId: UUID,
) : AbstractComposeView(composeView.context) {

    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val params = createLayoutParams()

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    private var viewShowing = false

    init {
        if (notTouchable) {
            setOnTouchListener { _, _ -> false }
        }

        id = android.R.id.content
        setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "CustomLayout:$uniqueId")
        setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
    }

    private var content: @Composable () -> Unit by mutableStateOf({})

    @Composable
    override fun Content() {
        content()
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    private fun createLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
        token = composeView.applicationWindowToken
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        format = PixelFormat.TRANSLUCENT

        if (focusable && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
    }

    fun show() {
        if (viewShowing) dismiss()
        windowManager.addView(this, params)

        if (focusable.not()) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        if (notTouchable) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        windowManager.updateViewLayout(this, params)
        viewShowing = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (focusable.not()) {
            return super.dispatchKeyEvent(event)
        }

        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBackClick()
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    fun dismiss() {
        if (!viewShowing) return
        disposeComposition()
        windowManager.removeViewImmediate(this)
        viewShowing = false
    }

    fun dispose() {
        dismiss()
        setViewTreeLifecycleOwner(null)
        setViewTreeSavedStateRegistryOwner(null)
        setViewTreeViewModelStoreOwner(null)
    }
}