package com.tangem.scenarios

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tangem.common.BaseTestCase
import com.tangem.screens.onSwapStoriesScreen
import io.github.kakaocup.kakao.intent.KIntent

fun openAppByDeepLink(deepLinkUri: String?) {
    requireNotNull(deepLinkUri) { "openAppByDeepLink: deepLinkUri is null" }
    val finalUri = Uri.parse(deepLinkUri)
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = Intent(ACTION_VIEW, finalUri).apply {
        // An unverified App Link (https://tangem.com/…) would otherwise resolve to the browser.
        setPackage(context.packageName)
        addFlags(FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}

/**
 * Opening Swap for the first time on a wallet shows a one-off "first-time-swap" stories overlay on
 * top of it (see [openSwapScreen]'s `storiesExist` handling) — whether it shows depends on a request
 * that can still be in flight here, so probe for it rather than assume either way.
 */
fun BaseTestCase.closeFirstTimeSwapStoriesIfShown() {
    val storiesShown = runCatching {
        awaitSuccess { onSwapStoriesScreen { closeButton.assertIsDisplayed() } }
    }.isSuccess

    if (storiesShown) {
        onSwapStoriesScreen { closeButton.performClick() }
    }
}

fun checkSendEMailIntentCalled() {
    val expectedIntent = KIntent {
        hasAction(ACTION_CHOOSER)
        hasExtra(EXTRA_TITLE, "Send mail...")
        hasExtraWithKey(EXTRA_INTENT)
    }
    expectedIntent.intended()
}