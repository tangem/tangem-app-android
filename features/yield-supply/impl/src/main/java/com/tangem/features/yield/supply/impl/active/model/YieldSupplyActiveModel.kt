package com.tangem.features.yield.supply.impl.active.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.*
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.active.entity.YieldSupplyActiveContentUM
import com.tangem.features.yield.supply.impl.active.model.transformers.YieldSupplyActiveFeeContentTransformer
import com.tangem.features.yield.supply.impl.active.model.transformers.YieldSupplyActiveMinAmountTransformer
import com.tangem.features.yield.supply.impl.subcomponents.approve.YieldSupplyApproveComponent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.TangemBlogUrlBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class YieldSupplyActiveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsHandler: AnalyticsEventHandler,
    private val yieldSupplyGetProtocolBalanceUseCase: YieldSupplyGetProtocolBalanceUseCase,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
    private val yieldSupplyMinAmountUseCase: YieldSupplyMinAmountUseCase,
    private val yieldSupplyGetCurrentFeeUseCase: YieldSupplyGetCurrentFeeUseCase,
    private val yieldSupplyGetMaxFeeUseCase: YieldSupplyGetMaxFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val urlOpener: UrlOpener,
    private val appRouter: AppRouter,
) : Model(), YieldSupplyStopEarningComponent.ModelCallback,
    YieldSupplyApproveComponent.ModelCallback {

    private val params: YieldSupplyActiveComponent.Params = paramsContainer.require()

    val slotNavigation = SlotNavigation<YieldSupplyActiveRoute>()

    lateinit var userWallet: UserWallet

    val cryptoCurrencyStatusFlow = MutableStateFlow(
        CryptoCurrencyStatus(
            value = CryptoCurrencyStatus.Loading,
            currency = params.cryptoCurrency,
        ),
    )

    val balanceHiddenFlow = MutableStateFlow(false)
    val transactionInProgressFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val userWalletId = params.userWalletId
    private val cryptoCurrency = cryptoCurrencyStatusFlow.value.currency
    private var appCurrency = AppCurrency.Default

    val uiState: StateFlow<YieldSupplyActiveContentUM>
        field = MutableStateFlow(
            YieldSupplyActiveContentUM(
                totalEarnings = stringReference("0"),
                availableBalance = null,
                providerTitle = resourceReference(R.string.yield_module_provider),
                subtitle = resourceReference(
                    id = R.string.yield_module_earn_sheet_provider_description,
                    formatArgs = wrappedList(cryptoCurrency.symbol, AAVEV3_PREFIX + cryptoCurrency.symbol),
                ),
                subtitleLink = resourceReference(R.string.common_read_more),
                notifications = persistentListOf(),
                minAmount = null,
                currentFee = null,
                feeDescription = null,
                isHighFee = false,
                minFeeDescription = null,
            ),
        )

    init {
        analyticsHandler.send(
            YieldSupplyAnalytics.EarnInProgressScreen(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        subscribeOnCurrencyStatusUpdates()

        modelScope.launch(dispatchers.default) {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            val protocolBalance = yieldSupplyGetProtocolBalanceUseCase(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
            ).getOrNull()

            stringReference(
                protocolBalance.format {
                    crypto(
                        symbol = AAVEV3_PREFIX + cryptoCurrency.symbol,
                        decimals = cryptoCurrency.decimals,
                    )
                },
            )
        }
    }

    override fun onDismissClick() {
        if (!transactionInProgressFlow.value) {
            slotNavigation.dismiss()
        }
    }

    override fun onTransactionProgress(inProgress: Boolean) {
        transactionInProgressFlow.update { inProgress }
    }

    override fun onStopEarningTransactionSent() {
        transactionInProgressFlow.update { false }
        appRouter.pop()
    }

    override fun onTransactionSent() {
        transactionInProgressFlow.update { false }
        appRouter.pop()
    }

    fun onApprove() {
        slotNavigation.activate(YieldSupplyActiveRoute.Approve)
    }

    fun onStopEarning() {
        slotNavigation.activate(YieldSupplyActiveRoute.Exit)
    }

    fun onReadMoreClick() {
        urlOpener.openUrl(TangemBlogUrlBuilder.YIELD_SUPPLY_HOW_IT_WORKS_URL)
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

                                val protocolBalance =
                                    cryptoCurrencyStatus.value.yieldSupplyStatus?.effectiveProtocolBalance
                                        ?: yieldSupplyGetProtocolBalanceUseCase(
                                            userWalletId = userWalletId,
                                            cryptoCurrency = cryptoCurrency,
                                        ).getOrNull()

                                loadApy()
                                loadMinAmount()
                                loadFees()

                                uiState.update {
                                    it.copy(
                                        availableBalance = stringReference(
                                            protocolBalance.format {
                                                crypto(
                                                    symbol = AAVEV3_PREFIX + cryptoCurrency.symbol,
                                                    decimals = cryptoCurrency.decimals,
                                                )
                                            },
                                        ),
                                    )
                                }
                            },
                            ifLeft = {
                                Timber.w(it.toString())
                            },
                        )
                    }.flowOn(dispatchers.default)
                        .launchIn(modelScope)
                },
                ifLeft = { error ->
                    Timber.w(error.toString())
                    return@launch
                },
            )
        }
    }

    private fun loadApy() {
        val cryptoCurrencyToken = cryptoCurrency as? CryptoCurrency.Token ?: return
        modelScope.launch(dispatchers.default) {
            yieldSupplyGetTokenStatusUseCase(cryptoCurrencyToken).onRight { tokenStatus ->
                uiState.update {
                    it.copy(
                        apy = TextReference.Str("${tokenStatus.apy}%"),
                    )
                }
            }.onLeft {
                uiState.update {
                    it.copy(
                        apy = TextReference.Str(DASH_SIGN),
                    )
                }
                Timber.e("Error loading token status")
            }
        }
    }

    private fun loadMinAmount() {
        modelScope.launch(dispatchers.default) {
            yieldSupplyMinAmountUseCase(
                userWalletId,
                cryptoCurrencyStatusFlow.value,
            ).onRight { minAmount ->
                uiState.update(
                    YieldSupplyActiveMinAmountTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value,
                        appCurrency = appCurrency,
                        minAmount = minAmount,
                        analyticsHandler = analyticsHandler,
                        onApprove = ::onApprove,
                    ),
                )
            }.onLeft {
                uiState.update {
                    it.copy(
                        minAmount = TextReference.Str(DASH_SIGN),
                        feeDescription = null,
                    )
                }
            }
        }
    }

    private fun loadFees() {
        modelScope.launch(dispatchers.default) {
            val cryptoStatus = cryptoCurrencyStatusFlow.value

            val currentFee = yieldSupplyGetCurrentFeeUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoStatus,
            ).getOrNull()

            val maxFee = yieldSupplyGetMaxFeeUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoStatus,
            ).getOrNull()

            if (currentFee != null && maxFee != null) {
                uiState.update(
                    YieldSupplyActiveFeeContentTransformer(
                        cryptoCurrencyStatus = cryptoStatus,
                        appCurrency = appCurrency,
                        feeValue = currentFee,
                        maxNetworkFee = maxFee,
                        analyticsHandler = analyticsHandler,
                    ),
                )
            } else {
                uiState.update {
                    it.copy(
                        currentFee = stringReference(DASH_SIGN),
                        feeDescription = null,
                        isHighFee = false,
                    )
                }
            }
        }
    }

    private companion object {
        const val AAVEV3_PREFIX = "a"
    }
}