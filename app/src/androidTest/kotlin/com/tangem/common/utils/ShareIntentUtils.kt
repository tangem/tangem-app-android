package com.tangem.common.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.VerificationModes
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo

/** Stubs the system share chooser so tapping 'Share' does not hand the test over to another app. */
fun stubShareChooserIntent() {
    Intents.intending(hasAction(Intent.ACTION_CHOOSER))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
}

/** Asserts the share chooser was opened [times] times in total, each carrying [text] as the shared text. */
fun assertTextSharedViaChooser(text: String, times: Int) {
    Intents.intended(
        allOf(
            hasAction(Intent.ACTION_CHOOSER),
            hasExtra(
                equalTo(Intent.EXTRA_INTENT),
                allOf(hasAction(Intent.ACTION_SEND), hasExtra(Intent.EXTRA_TEXT, text)),
            ),
        ),
        VerificationModes.times(times),
    )
}