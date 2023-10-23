package com.tangem.core.ui.extensions

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

fun Context.shareText(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    ContextCompat.startActivity(this, shareIntent, null)
}