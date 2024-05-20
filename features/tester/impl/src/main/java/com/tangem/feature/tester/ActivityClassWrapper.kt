package com.tangem.feature.tester

import android.app.Activity

/**
 * Wraps the main activity class to avoid type erasure issues during injection.
 *
 * @property clazz activity class
 */
class ActivityClassWrapper(
    val clazz: Class<out Activity>,
)