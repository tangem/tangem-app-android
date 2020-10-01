package com.tangem.tap.common

import android.app.Activity
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import kotlin.math.absoluteValue

class KeyboardObserver(activity: Activity) {

    private val decorView = activity.window.decorView
    private val windowManager = activity.windowManager
    private val originalWindowHeight: Int = getWindowHeight()
    private val onGlobalLayoutListener: OnGlobalLayoutListener = OnGlobalLayoutListener { onGlobalLayout() }

    private var onKeyboardListener: ((Boolean) -> Unit)? = null
    private var lastIsShow = false
    private var lastWindowHeight = getWindowHeight()

    fun registerListener(listener: (Boolean) -> Unit) {
        decorView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        onKeyboardListener = listener
    }

    fun unregisterListener() {
        decorView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        onKeyboardListener = null
    }

    private fun getWindowHeight() = Rect().apply { decorView.getWindowVisibleDisplayFrame(this) }.bottom

    private fun onGlobalLayout() {
        val currentWindowHeight = getWindowHeight()
        if (isSoftKeyChanged()) {
            lastWindowHeight = currentWindowHeight
            return
        }

        lastWindowHeight = currentWindowHeight
        val isShow = originalWindowHeight != currentWindowHeight
        if (lastIsShow == isShow) return

        lastIsShow = isShow
        onKeyboardListener?.invoke(isShow)
    }

    private fun isSoftKeyChanged() = ((lastWindowHeight - getWindowHeight()).absoluteValue) == getSoftKeyButtonHeight()

    private fun getSoftKeyButtonHeight(): Int {
        val applicationDisplayHeight = DisplayMetrics().apply {
            windowManager.defaultDisplay.getMetrics(this)
        }.heightPixels

        val realDisplayHeight = DisplayMetrics().apply {
            windowManager.defaultDisplay.getRealMetrics(this)
        }.heightPixels

        return realDisplayHeight - applicationDisplayHeight
    }

}