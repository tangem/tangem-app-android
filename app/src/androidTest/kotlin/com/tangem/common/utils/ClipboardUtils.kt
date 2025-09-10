package com.tangem.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun getClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return if (clipboard.hasPrimaryClip()) {
        clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    } else {
        null
    }
}

fun setClipboardText(context: Context, text: String?) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label", text)
    clipboard.setPrimaryClip(clip)
}