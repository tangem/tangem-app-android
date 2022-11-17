package com.tangem.tap.common.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.tangem.common.extensions.VoidCallback

fun Fragment.getDrawable(@DrawableRes drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(requireContext(), drawableResId)
}

fun Context.getDrawableCompat(@DrawableRes drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(this, drawableResId)
}

@ColorInt
fun Fragment.getColor(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(requireContext(), colorRes)
}

@ColorInt
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}

@ColorInt
fun View.getColor(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(context, colorRes)
}

fun View.getString(@StringRes id: Int): String {
    return context.getString(id)
}

fun View.getString(@StringRes id: Int, vararg formatArgs: String): String {
    return context.getString(id, *formatArgs)
}

fun View.getQuantityString(@PluralsRes id: Int, quantity: Int): String {
    return context.resources.getQuantityString(id, quantity, quantity)
}

fun View.getResourceName(): String {
    return try {
        resources.getResourceEntryName(id)
    } catch (ex: Resources.NotFoundException) {
        "Not found"
    }
}

fun View.show(show: Boolean, invokeBeforeStateChanged: (() -> Unit)? = null) {
    return if (show) this.show(invokeBeforeStateChanged)
    else this.hide(invokeBeforeStateChanged)
}

fun View.show(invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (this.visibility == View.VISIBLE) return

    invokeBeforeStateChanged?.invoke()
    this.visibility = View.VISIBLE
}

fun View.hide(invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (this.visibility == View.GONE) return

    invokeBeforeStateChanged?.invoke()
    this.visibility = View.GONE
}

fun View.invisible(invisible: Boolean = true, invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (invisible) {
        if (this.visibility == View.INVISIBLE) return

        invokeBeforeStateChanged?.invoke()
        this.visibility = View.INVISIBLE
    } else {
        this.show(invokeBeforeStateChanged)
    }
}

fun Context.dpToPixels(dp: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), this.resources.displayMetrics,
    ).toInt()

fun Context.pixelsToDp(pixels: Int): Int {
    return (pixels.toFloat() /
        (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT))
        .toInt()
}

fun Context.dpToPixels(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, this.resources.displayMetrics)

fun Context.pixelsToDp(pixels: Float): Float =
    (pixels / (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT))

tailrec fun Context?.getActivity(): Activity? = this as? Activity
    ?: (this as? ContextWrapper)?.baseContext?.getActivity()

fun MaterialCardView.setMargins(
    marginLeftDp: Int = 16,
    marginTopDp: Int = 8,
    marginRightDp: Int = 16,
    marginBottomDp: Int = 8,
) {
    val params = this.layoutParams
    (params as ViewGroup.MarginLayoutParams).setMargins(
        context.dpToPixels(marginLeftDp),
        context.dpToPixels(marginTopDp),
        context.dpToPixels(marginRightDp),
        context.dpToPixels(marginBottomDp),
    )
    this.layoutParams = params
}

fun Activity.setSystemBarTextColor(setTextDark: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val flags = this.window.decorView.systemUiVisibility
        // Update the SystemUiVisibility dependening on whether we want a Light or Dark theme.
        this.window.decorView.systemUiVisibility =
            if (setTextDark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
    }
}

fun View.hideKeyboard() {
    val inputMethodManager =
        context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(this.windowToken, 0)
}

fun Context.copyToClipboard(value: Any, label: String = "") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

    val clip: ClipData = ClipData.newPlainText(label, value.toString())
    clipboard.setPrimaryClip(clip)
}

fun Context.getFromClipboard(default: CharSequence? = null): CharSequence? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return default
    val clipData = clipboard.primaryClip ?: return default
    if (clipData.itemCount == 0) return default

    return clipData.getItemAt(0).text
}

fun Context.shareText(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

fun Fragment.shareText(text: String) {
    requireContext().shareText(text)
}

fun Context.safeStartActivity(
    intent: Intent,
    options: Bundle? = null,
    fallback: ((ActivityNotFoundException) -> Unit)? = null,
    finally: VoidCallback? = null,
) {
    try {
        this.startActivity(intent, options)
    } catch (ex: ActivityNotFoundException) {
        fallback?.invoke(ex)
    } finally {
        finally?.invoke()
    }
}

fun View.getString(resId: Int, vararg formatArgs: Any?): String {
    return context.getString(resId, *formatArgs)
}

fun View.animateVisibility(
    show: Boolean,
    durationMillis: Long = SHORT_ANIMATION_DURATION,
    hiddenVisibility: Int = View.GONE,
) {
    if (show) {
        this.animate()
            .alpha(1f)
            .setDuration(durationMillis)
            .withStartAction {
                this.alpha = 0f
                this.isVisible = true
            }
    } else {
        this.animate()
            .alpha(0f)
            .setDuration(durationMillis)
            .withStartAction {
                this.visibility = hiddenVisibility
            }
    }
}

private const val SHORT_ANIMATION_DURATION = 80L
