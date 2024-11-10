package com.tangem.features.onramp.swap.model

import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.entity.SwapSelectTokensController
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.swap.entity.transformer.RemoveSelectedFromTokenTransformer
import com.tangem.features.onramp.swap.entity.transformer.SelectFromTokenTransformer
import com.tangem.features.onramp.swap.entity.transformer.SelectToTokenTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Suppress("LongParameterList")
internal class SwapSelectTokensModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val controller: SwapSelectTokensController,
) : Model() {

    val state: StateFlow<SwapSelectTokensUM> = controller.state

    val fromCurrencyStatus: StateFlow<CryptoCurrencyStatus?> get() = _fromCurrencyStatus

    private val _fromCurrencyStatus = MutableStateFlow<CryptoCurrencyStatus?>(value = null)
    private val _toCurrencyStatus = MutableStateFlow<CryptoCurrencyStatus?>(value = null)

    /**
     * Select "from" token
     *
     * @param selectedTokenItemState selected token item state
     * @param status                 crypto currency status
     */
    fun selectFromToken(selectedTokenItemState: TokenItemState, status: CryptoCurrencyStatus) {
        _fromCurrencyStatus.value = status

        controller.update(
            transformer = SelectFromTokenTransformer(
                selectedTokenItemState = selectedTokenItemState,
                onRemoveClick = ::onRemoveClick,
            ),
        )
    }

    /**
     * Select "to" token
     *
     * @param selectedTokenItemState selected token item state
     * @param status                 crypto currency status
     */
    fun selectToToken(selectedTokenItemState: TokenItemState, status: CryptoCurrencyStatus) {
        _toCurrencyStatus.value = status

        controller.update(transformer = SelectToTokenTransformer(selectedTokenItemState))

        // TODO: https://tangem.atlassian.net/browse/AND-8989
    }

    private fun onRemoveClick() {
        _fromCurrencyStatus.value = null

        controller.update(transformer = RemoveSelectedFromTokenTransformer)
    }
}
