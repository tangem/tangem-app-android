package com.tangem.tap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
internal class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.e("onReceive: $context, $intent")
        context?.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
        )
    }
}