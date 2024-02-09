package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenActionButtonConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletCurrencyActionsClickIntentsImplementor
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class MultiWalletCurrencyActionsConverter(
    private val userWallet: UserWallet,
    private val clickIntents: WalletCurrencyActionsClickIntentsImplementor,
) : Converter<TokenActionsState, ImmutableList<TokenActionButtonConfig>> {

    override fun convert(value: TokenActionsState): ImmutableList<TokenActionButtonConfig> {
        return value.states
            .filterIfSingleWithToken()
            .mapNotNull {
                mapTokenActionState(actionsState = it, cryptoCurrencyStatus = value.cryptoCurrencyStatus)
            }
            .toImmutableList()
    }

    private fun List<TokenActionsState.ActionState>.filterIfSingleWithToken(): List<TokenActionsState.ActionState> {
        return if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            filter { it !is TokenActionsState.ActionState.HideToken }
        } else {
            this
        }
    }

    private fun mapTokenActionState(
        actionsState: TokenActionsState.ActionState,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenActionButtonConfig? {
        if (actionsState is TokenActionsState.ActionState.Send && cryptoCurrencyStatus.value.amount.isNullOrZero()) {
            return null
        }

        val title: TextReference
        val icon: Int
        val action: () -> Unit
        when (actionsState) {
            is TokenActionsState.ActionState.Buy -> {
                title = resourceReference(R.string.common_buy)
                icon = R.drawable.ic_plus_24
                action = { clickIntents.onBuyClick(cryptoCurrencyStatus) }
            }
            is TokenActionsState.ActionState.Receive -> {
                title = resourceReference(R.string.common_receive)
                icon = R.drawable.ic_arrow_down_24
                action = { clickIntents.onReceiveClick(cryptoCurrencyStatus) }
            }
            is TokenActionsState.ActionState.Sell -> {
                title = resourceReference(R.string.common_sell)
                icon = R.drawable.ic_currency_24
                action = { clickIntents.onSellClick(cryptoCurrencyStatus) }
            }
            is TokenActionsState.ActionState.Send -> {
                title = resourceReference(R.string.common_send)
                icon = R.drawable.ic_arrow_up_24
                action = { clickIntents.onSendClick(cryptoCurrencyStatus) }
            }
            is TokenActionsState.ActionState.Swap -> {
                title = resourceReference(R.string.swapping_swap_action)
                icon = R.drawable.ic_exchange_horizontal_24
                action = { clickIntents.onSwapClick(cryptoCurrencyStatus) }
            }
            is TokenActionsState.ActionState.CopyAddress -> {
                title = resourceReference(R.string.common_copy_address)
                icon = R.drawable.ic_copy_24
                action = { clickIntents.onCopyAddressClick(cryptoCurrencyStatus) }
            }
            is TokenActionsState.ActionState.HideToken -> {
                title = resourceReference(R.string.token_details_hide_token)
                icon = R.drawable.ic_hide_24
                action = { clickIntents.onHideTokensClick(cryptoCurrencyStatus) }
            }
        }

        return TokenActionButtonConfig(
            text = title,
            iconResId = icon,
            onClick = action,
            isWarning = actionsState is TokenActionsState.ActionState.HideToken,
            enabled = actionsState.enabled,
        )
    }
}
