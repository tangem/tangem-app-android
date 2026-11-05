package com.tangem.common.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.VerificationModes
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo

/** Stubs the system share chooser so tapping 'Share' does not hand the test over to another app. */
fun stubShareChooserIntent() {
    Intents.intending(hasAction(Intent.ACTION_CHOOSER))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
}

/**
 * Returns the text the most recent share chooser carried (the `EXTRA_TEXT` of its wrapped `ACTION_SEND`), or null
 * if no share chooser was recorded. Pair with [stubShareChooserIntent] so the chooser is swallowed and recorded
 * rather than handed to another app. Use when the shared text is produced at runtime (e.g. a transaction explorer
 * URL) and so can't be matched up-front with [assertTextSharedViaChooser].
 */
@Suppress("DEPRECATION")
fun lastSharedChooserText(): String? {
    return Intents.getIntents()
        .lastOrNull { it.action == Intent.ACTION_CHOOSER }
        ?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        ?.getStringExtra(Intent.EXTRA_TEXT)
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

/** Asserts the share chooser was opened [times] times in total, each sharing a text that contains every [fragments]. */
fun assertTextContainingSharedViaChooser(fragments: List<String>, times: Int) {
    Intents.intended(
        allOf(
            hasAction(Intent.ACTION_CHOOSER),
            hasExtra(
                equalTo(Intent.EXTRA_INTENT),
                allOf(
                    hasAction(Intent.ACTION_SEND),
                    hasExtra(equalTo(Intent.EXTRA_TEXT), allOf(fragments.map(::containsString))),
                ),
            ),
        ),
        VerificationModes.times(times),
    )
}