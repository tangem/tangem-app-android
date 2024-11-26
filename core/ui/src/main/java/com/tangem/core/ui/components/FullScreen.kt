package com.tangem.core.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
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
fun FullScreen(notTouchable: Boolean = false, content: @Composable (() -> Unit) -> Unit) {
    val view = LocalView.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val id = rememberSaveable { UUID.randomUUID() }

    val fullScreenLayout = remember {
        FullScreenLayout(
            notTouchable = notTouchable,
            composeView = view,
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
    private val composeView: View,
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
        flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    fun show() {
        if (viewShowing) dismiss()
        windowManager.addView(this, params)
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        if (notTouchable) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        windowManager.updateViewLayout(this, params)
        viewShowing = true
    }

    fun dismiss() {
        if (!viewShowing) return
        disposeComposition()
        windowManager.removeViewImmediate(this)
        viewShowing = false
    }

    fun dispose() {
        dismiss()
        setViewTreeSavedStateRegistryOwner(null)
        setViewTreeSavedStateRegistryOwner(null)
        setViewTreeViewModelStoreOwner(null)
    }
}
