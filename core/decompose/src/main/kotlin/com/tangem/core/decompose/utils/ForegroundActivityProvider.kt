package com.tangem.core.decompose.utils

import androidx.appcompat.app.AppCompatActivity

/**
 * Provides the activity currently in the foreground, if there is one.
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