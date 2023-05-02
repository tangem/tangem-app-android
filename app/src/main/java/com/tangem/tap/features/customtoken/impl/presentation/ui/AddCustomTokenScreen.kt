package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.compose.runtime.Composable
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder

/**
 * Add custom token screen
 *
 * @param stateHolder state holder
 *
* [REDACTED_AUTHOR]
 */
@Suppress("UnusedPrivateMember")
@Composable
internal fun AddCustomTokenScreen(stateHolder: AddCustomTokenStateHolder) {
    when (stateHolder) {
        is AddCustomTokenStateHolder.Content -> AddCustomTokenContent(state = stateHolder)
        is AddCustomTokenStateHolder.TestContent -> AddCustomTokenTestContent(state = stateHolder)
    }
}
