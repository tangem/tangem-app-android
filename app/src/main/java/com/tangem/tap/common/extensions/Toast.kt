package com.tangem.tap.common.extensions

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Context.toast(message: String, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, length).show()
}

fun Context.toast(@StringRes messageRes: Int, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, this.getString(messageRes), length).show()
}

fun View.toast(message: String, length: Int = Toast.LENGTH_LONG) {
    context.toast(message, length)
}

fun View.toast(@StringRes messageRes: Int, length: Int = Toast.LENGTH_LONG) {
    context.toast(context.getString(messageRes), length)
}

fun Fragment.toast(message: String, length: Int = Toast.LENGTH_LONG) {
    context?.toast(message, length)
}

fun Fragment.toast(@StringRes messageRes: Int, length: Int = Toast.LENGTH_LONG) {
    context?.let { it.toast(it.getString(messageRes), length) }
}