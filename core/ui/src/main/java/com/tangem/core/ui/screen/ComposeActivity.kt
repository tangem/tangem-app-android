package com.tangem.core.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * An abstract base class for activities that use Compose for UI rendering.
 * Extends [ComponentActivity] and implements [ComposeScreen] interface.
 */
abstract class ComposeActivity : ComponentActivity(), ComposeScreen {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createComposeView(context = this, activity = this))
    }
}
