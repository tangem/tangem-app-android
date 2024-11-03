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
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Suppress("LongParameterList")
internal class SwapSelectTokensModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val controller: SwapSelectTokensController,
) : Model() {

    val state: StateFlow<SwapSelectTokensUM> = controller.state

    private var fromCurrencyStatus: CryptoCurrencyStatus? = null
    private var toCurrencyStatus: CryptoCurrencyStatus? = null

    /**
     * Select "from" token
     *
     * @param selectedTokenItemState selected token item state
     * @param status                 crypto currency status
     */
    fun selectFromToken(selectedTokenItemState: TokenItemState, status: CryptoCurrencyStatus) {
        fromCurrencyStatus = status

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
        toCurrencyStatus = status

        controller.update(transformer = SelectToTokenTransformer(selectedTokenItemState))

        // TODO: [REDACTED_JIRA]
    }

    private fun onRemoveClick() {
        fromCurrencyStatus = null

        controller.update(transformer = RemoveSelectedFromTokenTransformer)
    }
}