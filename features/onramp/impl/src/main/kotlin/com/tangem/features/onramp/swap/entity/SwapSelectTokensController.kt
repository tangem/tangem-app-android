package com.tangem.features.onramp.swap.entity

import com.tangem.features.onramp.swap.entity.utils.createEmptyExchangeFrom
import com.tangem.features.onramp.swap.entity.utils.createEmptyExchangeTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * [SwapSelectTokensUM] controller
 *
[REDACTED_AUTHOR]
 */
internal class SwapSelectTokensController @Inject constructor() {

    val state: StateFlow<SwapSelectTokensUM>
        field = MutableStateFlow(
            value = SwapSelectTokensUM(
                onBackClick = {},
                exchangeFrom = createEmptyExchangeFrom(),
                exchangeTo = createEmptyExchangeTo(),
                isBalanceHidden = false,
            ),
        )

    fun update(transform: (SwapSelectTokensUM) -> SwapSelectTokensUM) {
        Timber.d("Applying non-name transformation")
        state.update(transform)
    }

    fun update(transformer: SwapSelectTokensUMTransformer) {
        Timber.d("Applying ${transformer::class.simpleName ?: "null"}")
        state.update(transformer::transform)
    }
}