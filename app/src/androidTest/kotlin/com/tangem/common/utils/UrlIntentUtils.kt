package com.tangem.common.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.VerificationModes
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasDataString
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo

/** Stubs opening [url] so the browser / Custom Tab never starts and the test stays in the app. */
fun stubUrlOpening(url: String) {
    Intents.intending(allOf(hasAction(Intent.ACTION_VIEW), hasDataString(equalTo(url))))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
}

/** Asserts [url] was opened [times] times in total, via a browser or a Custom Tab. */
fun assertUrlOpened(url: String, times: Int = 1) {
    Intents.intended(
        allOf(hasAction(Intent.ACTION_VIEW), hasDataString(equalTo(url))),
        VerificationModes.times(times),
    )
}