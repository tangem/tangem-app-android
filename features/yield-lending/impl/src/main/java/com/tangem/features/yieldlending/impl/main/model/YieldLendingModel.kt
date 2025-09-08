package com.tangem.features.yieldlending.impl.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.yieldlending.api.YieldLendingComponent
import com.tangem.features.yieldlending.impl.main.entity.YieldLendingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@ModelScoped
internal class YieldLendingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val appRouter: AppRouter,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
) : Model(), YieldLendingClickIntents {

    private val params = paramsContainer.require<YieldLendingComponent.Params>()

    val uiState: StateFlow<YieldLendingUM>
        field = MutableStateFlow<YieldLendingUM>(YieldLendingUM.Loading)

    val bottomSheetNavigation: SlotNavigation<Unit> = SlotNavigation()

    private val cryptoCurrency = params.cryptoCurrency
    var userWallet: UserWallet by Delegates.notNull()

    val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                params.cryptoCurrency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    init {
        subscribeOnCurrencyStatusUpdates()
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(params.userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet

                    getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                        userWalletId = params.userWalletId,
                        currencyId = cryptoCurrency.id,
                        isSingleWalletWithTokens = false,
                    ).onEach { maybeCryptoCurrency ->
                        maybeCryptoCurrency.fold(
                            ifRight = { cryptoCurrencyStatus ->
                                cryptoCurrencyStatusFlow.update { cryptoCurrencyStatus }

                                if (cryptoCurrencyStatus.value.yieldLendingStatus != null) {
                                    uiState.update { YieldLendingUM.Content }
                                } else {
                                    uiState.update {
                                        YieldLendingUM.Initial
                                    }
                                }
                            },
                            ifLeft = {
                                //todo error
                            },
                        )
                    }.launchIn(modelScope)
                },
                ifLeft = {
                    Timber.w(it.toString())
                    return@launch
                },
            )
        }
    }

    override fun onClick() {
        when (uiState.value) {
            YieldLendingUM.Content -> bottomSheetNavigation.activate(Unit)
            YieldLendingUM.Initial -> {
                appRouter.push(
                    AppRoute.YieldLendingPromo(
                        userWalletId = params.userWalletId,
                        cryptoCurrency = params.cryptoCurrency,
                    )
                )
            }
            YieldLendingUM.Loading -> TODO()
        }
    }
}