package com.tangem.features.onramp.swap.entity

import com.tangem.core.decompose.navigation.Router
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
 * @property router router
 *
 * @author Andrew Khokhlov on 02/11/2024
 */
internal class SwapSelectTokensController @Inject constructor(
    private val router: Router,
) {

    val state: StateFlow<SwapSelectTokensUM> get() = _state

    private val _state: MutableStateFlow<SwapSelectTokensUM> = MutableStateFlow(
        value = SwapSelectTokensUM(
            onBackClick = router::pop,
            exchangeFrom = createEmptyExchangeFrom(),
            exchangeTo = createEmptyExchangeTo(),
        ),
    )

    fun update(transform: (SwapSelectTokensUM) -> SwapSelectTokensUM) {
        Timber.d("Applying non-name transformation")
        _state.update(transform)
    }

    fun update(transformer: SwapSelectTokensUMTransformer) {
        Timber.d("Applying ${transformer::class.simpleName}")
        _state.update(transformer::transform)
    }
}
