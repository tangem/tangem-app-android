package com.tangem.tap.features.intentHandler

import android.content.Intent

/**
 * @author Anton Zhilenkov on 04.06.2023.
 */
interface IntentHandler {

    fun handleIntent(intent: Intent?, isFromForeground: Boolean): Boolean
}
