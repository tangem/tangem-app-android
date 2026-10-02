package com.tangem.common.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.Matchers.hasToString

fun stubExternalUrlOpening() {
    Intents.intending(hasAction(Intent.ACTION_VIEW))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
}

fun assertUrlOpenedStartingWith(urlPrefix: String) {
    Intents.intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(hasToString<Uri>(startsWith(urlPrefix)))))
}