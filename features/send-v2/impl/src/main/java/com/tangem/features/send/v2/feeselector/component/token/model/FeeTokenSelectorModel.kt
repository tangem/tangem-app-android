package com.tangem.features.send.v2.feeselector.component.token.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.token.FeeTokenSelectorIntents
import com.tangem.features.send.v2.feeselector.component.token.entity.FeeTokenSelectorUM
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeTokenSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model(), FeeTokenSelectorIntents,
    FeeSelectorIntents by paramsContainer.require<FeeSelectorComponentParams>().intents {

    private val params = paramsContainer.require<FeeSelectorComponentParams>()
    private var appCurrency = AppCurrency.Default

    val uiState: StateFlow<FeeTokenSelectorUM>
        field = MutableStateFlow(getInitialState())

    init {
        initAppCurrency()

        params.state
            .onEach { uiState.value = stateFromParent(it as FeeSelectorUM.Content) }
            .launchIn(modelScope)
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun getInitialState(): FeeTokenSelectorUM {
        val parentState = params.state.value as FeeSelectorUM.Content
        return stateFromParent(parentState)
    }

    private fun stateFromParent(newParentState: FeeSelectorUM.Content): FeeTokenSelectorUM {
        val parentState = newParentState
        val selectedCurrencyStatus = parentState.feeExtraInfo.feeCryptoCurrencyStatus
        val selectedTokenUM = convertToken(selectedCurrencyStatus)

        return FeeTokenSelectorUM(
            parent = parentState,
            selectedToken = selectedTokenUM,
            tokens = parentState.feeExtraInfo.availableFeeCurrencies?.map { status ->
                convertToken(status)
            }?.toImmutableList() ?: persistentListOf(selectedTokenUM),
        )
    }

    private fun convertToken(status: CryptoCurrencyStatus): TokenItemState {
        return FeeTokenForListConverter(
            appCurrencyProvider = Provider { appCurrency },
            onTokenClick = { onTokenSelected(status) },
        ).convert(status)
    }

    override fun onLearnMoreClick() {
        modelScope.launch {
            urlOpener.openUrl(
                TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.WhatIsTransactionFee),
            )
        }
    }
}