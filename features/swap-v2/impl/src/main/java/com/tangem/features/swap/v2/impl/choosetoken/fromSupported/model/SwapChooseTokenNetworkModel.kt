package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.managetokens.CreateCryptoCurrencyUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.domain.swap.usecase.GetSwapSupportedPairsUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.managetokens.component.analytics.CommonManageTokensAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkComponent
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenFactory.getErrorMessage
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers.SwapChooseContentStateTransformer
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers.SwapChooseErrorStateTransformer
import com.tangem.features.swap.v2.impl.common.SwapUtils.SEND_WITH_SWAP_PROVIDER_TYPES
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class SwapChooseTokenNetworkModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getSwapSupportedPairsUseCase: GetSwapSupportedPairsUseCase,
    private val createCryptoCurrencyUseCase: CreateCryptoCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val swapChooseTokenAlertFactory: SwapChooseTokenAlertFactory,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: SwapChooseTokenNetworkComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SwapChooseTokenNetworkUM>
        field: MutableStateFlow<SwapChooseTokenNetworkUM> = MutableStateFlow(
            SwapChooseTokenNetworkUM(
                TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = params.onDismiss,
                    content = SwapChooseTokenNetworkContentUM.Loading(
                        messageContent = getErrorMessage(
                            tokenName = params.token.name,
                            onDismiss = params.onDismiss,
                        ),
                    ),
                ),
            ),
        )

    init {
        initContent()
    }

    private fun initContent() {
        val userWallet = getUserWalletUseCase(params.userWalletId).getOrElse {
            Timber.e("Failed to get user wallet: $it")
            swapChooseTokenAlertFactory.getGenericErrorState(params.onDismiss)
            return
        }

        modelScope.launch {
            val cryptoCurrencyList = createCryptoCurrencyUseCase(
                token = params.token,
                userWalletId = params.userWalletId,
            ).getOrElse {
                Timber.e("Failed to get crypto currency")
                swapChooseTokenAlertFactory.getGenericErrorState(params.onDismiss)
                return@launch
            }
            val pairs = getSwapSupportedPairsUseCase.invoke(
                userWallet = userWallet,
                initialCurrency = params.initialCurrency,
                cryptoCurrencyList = cryptoCurrencyList + params.initialCurrency,
                filterProviderTypes = SEND_WITH_SWAP_PROVIDER_TYPES,
                swapTxType = SwapTxType.SendWithSwap,
            ).getOrElse {
                Timber.e(it.toString())
                uiState.update(
                    SwapChooseErrorStateTransformer(
                        tokenName = params.token.name,
                        onDismiss = params.onDismiss,
                    ),
                )
                return@launch
            }
            delay(MINIMUM_LOADING_TIME)
            if (pairs.fromGroup.available.isEmpty()) {
                analyticsEventHandler.send(
                    SendWithSwapAnalyticEvents.NoticeCanNotSwapToken(
                        fromToken = params.initialCurrency,
                        toTokenSymbol = params.token.symbol,
                    ),
                )
            }
            uiState.update(
                SwapChooseContentStateTransformer(
                    pairs = pairs,
                    onNetworkClick = ::onSwapTokenClick,
                    tokenName = params.token.name,
                    onDismiss = params.onDismiss,
                ),
            )
        }
    }

    private fun onSwapTokenClick(swapCurrencies: SwapCurrencies, cryptoCurrency: CryptoCurrency) {
        val prevSelectedCurrency = params.selectedCurrency?.network
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.TokenChosen(
                categoryName = params.analyticsCategoryName,
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        if (params.isSearchedToken) {
            analyticsEventHandler.send(
                CommonManageTokensAnalyticEvents.TokenSearched(
                    categoryName = params.analyticsCategoryName,
                    token = cryptoCurrency.symbol,
                    blockchain = cryptoCurrency.network.name,
                    isTokenChosen = true,
                ),
            )
        }
        if (prevSelectedCurrency == null || cryptoCurrency.network == prevSelectedCurrency) {
            params.onResult(swapCurrencies, cryptoCurrency)
        } else {
            swapChooseTokenAlertFactory.showChangeTokenAlert(
                onConfirm = {
                    params.onResult(swapCurrencies, cryptoCurrency)
                },
                onDismiss = params.onDismiss,
            )
        }
    }

    private companion object {
        const val MINIMUM_LOADING_TIME = 100L
    }
}