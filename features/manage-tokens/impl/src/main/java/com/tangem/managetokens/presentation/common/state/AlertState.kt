package com.tangem.managetokens.presentation.common.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.managetokens.impl.R

internal sealed class AlertState {

    abstract val message: TextReference

    class DefaultAlert(
        override val message: TextReference,
    ) : AlertState()

    class TokenUnavailable(
        val onUpvoteClick: () -> Unit,
    ) : AlertState() {
        override val message: TextReference = resourceReference(R.string.manage_tokens_unavailable_description)
        val confirmButtonText: TextReference = resourceReference(R.string.common_close)
        val dismissButtonText: TextReference = resourceReference(R.string.manage_tokens_unavailable_vote)
    }

    object NonNative : AlertState() {
        override val message: TextReference = resourceReference(R.string.manage_tokens_network_selector_non_native_info)
    }

    class TokensUnsupported(networkName: String) : AlertState() {
        override val message: TextReference = resourceReference(
            id = R.string.alert_manage_tokens_unsupported_message,
            formatArgs = wrappedList(networkName),
        )
    }

    object TokensUnsupportedCurve : AlertState() {
        override val message: TextReference = resourceReference(R.string.alert_manage_tokens_unsupported_curve_message)
    }

    class TokensUnsupportedBlockchainByCard(networkName: String) : AlertState() {
        override val message: TextReference = resourceReference(
            id = R.string.alert_manage_tokens_unsupported_blockchain_by_card_message,
            formatArgs = wrappedList(networkName),
        )
    }

    class CannotHideNetworkWithTokens(tokenName: String, currencySymbol: String, networkName: String) : AlertState() {
        override val message: TextReference = resourceReference(
            id = R.string.token_details_unable_hide_alert_message,
            formatArgs = wrappedList(tokenName, currencySymbol, networkName),
        )
    }

    object TokenAlreadyAdded : AlertState() {
        override val message: TextReference = resourceReference(R.string.custom_token_validation_error_already_added)
    }
}