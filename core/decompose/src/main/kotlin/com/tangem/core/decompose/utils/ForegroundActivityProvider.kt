package com.tangem.core.decompose.utils

import androidx.appcompat.app.AppCompatActivity

/**
 * Provides the most recently resumed activity that is not destroyed yet, if there is one. It stays available while
 * the app is in the background, so it does not tell whether the app is visible.
 *
 * Unlike [ActivityHolder], which is bound to a component and always has an activity, this is meant
 * for application-scoped dependencies that have no component context to take the activity from —
 * hence the nullable result.
 *
 * Prefer [ActivityHolder] whenever the caller lives inside a component.
 */
interface ForegroundActivityProvider {
    val foregroundActivity: AppCompatActivity?
}

fun ForegroundActivityProvider.withForegroundActivity(block: (AppCompatActivity) -> Unit) {
    foregroundActivity?.let { block(it) }
}