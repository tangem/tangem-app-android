package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.features.foryou.impl.tokensummary.model.SwapHolding
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Pairs a holding with what a swap from it would run into, taken from [GetCryptoCurrencyActionsUseCase] — the same
 * source the Swap button uses everywhere else.
 *
 * The result is a flow because the use case reports actions as they settle. The swap scenario itself depends only on
 * the wallet and the status, both fixed for a holding, so the first value it reports is already the final one.
 */
internal class SwapHoldingConverter(
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
) : Converter<TokenSelectorEntry, Flow<SwapHolding>> {

    override fun convert(value: TokenSelectorEntry): Flow<SwapHolding> =
        getCryptoCurrencyActionsUseCase(userWallet = value.wallet, cryptoCurrencyStatus = value.currencyStatus)
            .map { actions ->
                SwapHolding(entry = value, unavailabilityReason = actions.resolveSwapReason())
            }

    /**
     * Reason of the holding's Swap action, or a generic one when no Swap action is offered at all — a status the use
     * case has no swap scenario for, a missed derivation among them, yields nothing to read a reason from.
     */
    private fun TokenActionsState.resolveSwapReason(): ScenarioUnavailabilityReason =
        states.firstOrNull { it is TokenActionsState.ActionState.Swap }
            ?.unavailabilityReason
            ?: ScenarioUnavailabilityReason.Unreachable
}