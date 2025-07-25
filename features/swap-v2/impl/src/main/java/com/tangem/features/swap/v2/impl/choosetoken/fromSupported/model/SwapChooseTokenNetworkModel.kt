package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.managetokens.CreateCryptoCurrencyUseCase
import com.tangem.domain.swap.usecase.GetSwapSupportedPairsUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkComponent
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenFactory.getErrorMessage
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers.SwapChooseContentStateTransformer
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers.SwapChooseErrorStateTransformer
import com.tangem.features.swap.v2.impl.common.SwapUtils.SEND_WITH_SWAP_PROVIDER_TYPES
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class SwapChooseTokenNetworkModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getSwapSupportedPairsUseCase: GetSwapSupportedPairsUseCase,
    private val createCryptoCurrencyUseCase: CreateCryptoCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val uiMessageSender: UiMessageSender,
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
            getGenericErrorState()
            return
        }

        modelScope.launch {
            val cryptoCurrencyList = createCryptoCurrencyUseCase(
                token = params.token,
                userWalletId = params.userWalletId,
            ).getOrElse {
                Timber.e("Failed to get crypto currency")
                getGenericErrorState()
                return@launch
            }
            val pairs = getSwapSupportedPairsUseCase.invoke(
                userWallet = userWallet,
                initialCurrency = params.initialCurrency,
                cryptoCurrencyList = cryptoCurrencyList + params.initialCurrency,
                filterProviderTypes = SEND_WITH_SWAP_PROVIDER_TYPES,
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
            uiState.update(
                SwapChooseContentStateTransformer(
                    pairs = pairs,
                    onNetworkClick = params.onResult,
                    tokenName = params.token.name,
                    onDismiss = params.onDismiss,
                ),
            )
        }
    }

    private fun getGenericErrorState() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.common_error),
                message = resourceReference(id = R.string.common_unknown_error),
                onDismissRequest = params.onDismiss,
                firstActionBuilder = { okAction() },
            ),
        )
    }

    private companion object {
        const val MINIMUM_LOADING_TIME = 100L
    }
}