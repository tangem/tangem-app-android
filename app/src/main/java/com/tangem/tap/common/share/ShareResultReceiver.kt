package com.tangem.tap.common.share

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Called by the system share sheet once the user picks where to send a text shared by [IntentShareManager] */
@AndroidEntryPoint
internal class ShareResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var shareManager: IntentShareManager

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_SHARED_TEXT) ?: return

        shareManager.onShareTargetChosen(text)
    }

    companion object {

        private const val EXTRA_SHARED_TEXT = "shared_text"

        fun intent(context: Context, text: String): Intent = Intent(context, ShareResultReceiver::class.java)
            .putExtra(EXTRA_SHARED_TEXT, text)
    }
}