package com.tangem.core.decompose.utils

import androidx.appcompat.app.AppCompatActivity

/**
 * Interface for holding an [AppCompatActivity] instance.
 *
 * This interface is used to provide access to the activity in which the component is running.
 */
interface ActivityHolder {

    /**
     * The [AppCompatActivity] instance associated with this context.
     */
    val activity: AppCompatActivity
}