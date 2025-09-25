package com.tangem.scenarios

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.github.kakaocup.kakao.intent.KIntent

fun openAppByDeepLink(deepLinkUri: String?) {
    val deeplinkScheme = "tangem://wc?uri="
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = Intent(ACTION_VIEW, Uri.parse(deeplinkScheme + deepLinkUri)).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}

fun checkSendEMailIntentCalled() {
    val expectedIntent = KIntent {
        hasAction(ACTION_CHOOSER)
        hasExtra(EXTRA_TITLE, "Send mail...")
        hasExtraWithKey(EXTRA_INTENT)
    }
    expectedIntent.intended()
}