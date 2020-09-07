package com.tangem.tap.common.extensions

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

fun Fragment.getDrawable(@DrawableRes drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(requireContext(), drawableResId)
}

fun Context.getDrawableCompat(@DrawableRes drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(this, drawableResId)
}

fun View.show(show: Boolean) {
    if (show) this.visibility = View.VISIBLE else this.visibility = View.GONE
}

fun View.show() {
    if (this.visibility == View.VISIBLE) return
    this.visibility = View.VISIBLE
}

fun View.hide() {
    if (this.visibility == View.GONE) return
    this.visibility = View.GONE
}

fun View.makeInvisible() {
    if (this.visibility == View.INVISIBLE) return
    this.visibility = View.INVISIBLE
}

fun Context.dpToPixels(dp: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), this.resources.displayMetrics
    ).toInt()

fun MaterialCardView.setMargins(
    marginLeftDp: Int = 16,
    marginTopDp: Int = 8,
    marginRightDp: Int = 16,
    marginBottomDp: Int = 8
) {
    val params = this.layoutParams
    (params as ViewGroup.MarginLayoutParams).setMargins(
        context.dpToPixels(marginLeftDp),
        context.dpToPixels(marginTopDp),
        context.dpToPixels(marginRightDp),
        context.dpToPixels(marginBottomDp)
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



fun String.colorSegment(
    context: Context,
    color: Int,
    startIndex: Int = 0,
    endIndex: Int = this.length
): Spannable {
    return this.toSpannable()
        .also { spannable ->
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, color)),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(this.windowToken, 0)
}

fun Context.copyToClipboard(value: Any, label: String = "") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

    val clip: ClipData = ClipData.newPlainText(label, value.toString())
    clipboard.setPrimaryClip(clip)
}

fun Context.getFromClipboard(default: CharSequence? = null): CharSequence? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return default
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

fun ViewGroup.inflate(viewToInflate: Int, rootView: ViewGroup?, parent: ViewGroup) {
    if (rootView == null) {
        val inflatedView = LayoutInflater.from(context).inflate(viewToInflate, rootView)
        parent.addView(inflatedView)
    }
}