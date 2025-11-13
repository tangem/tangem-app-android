package com.tangem.features.yield.supply.impl.subcomponents.active.model

import arrow.core.getOrElse
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.notifications.NotificationConfig
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
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetProtocolBalanceUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import com.tangem.features.yield.supply.impl.subcomponents.active.model.transformers.YieldSupplyActiveMinAmountTransformer
import com.tangem.features.yield.supply.impl.subcomponents.active.model.transformers.YieldSupplyActiveFeeContentTransformer
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
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
) : Model() {

    private val params: YieldSupplyActiveComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow
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
                    formatArgs = wrappedList(cryptoCurrency.symbol, cryptoCurrency.symbol),
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
        subscribeOnCurrencyUpdates()

        modelScope.launch(dispatchers.default) {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            val protocolBalance = yieldSupplyGetProtocolBalanceUseCase(
                userWalletId = params.userWallet.walletId,
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

    private fun subscribeOnCurrencyUpdates() {
        cryptoCurrencyStatusFlow.onEach { cryptoCurrencyStatus ->
            val protocolBalance = cryptoCurrencyStatus.value.yieldSupplyStatus?.effectiveProtocolBalance
                ?: yieldSupplyGetProtocolBalanceUseCase(
                    userWalletId = params.userWallet.walletId,
                    cryptoCurrency = cryptoCurrency,
                ).getOrNull()

            loadApy()
            loadMinAmount()
            loadFees()

            uiState.update {
                it.copy(
                    notifications = getNotifications(cryptoCurrencyStatus),
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
        }.flowOn(dispatchers.default)
            .launchIn(modelScope)
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

    private fun getNotifications(cryptoCurrencyStatus: CryptoCurrencyStatus): ImmutableList<NotificationUM> {
        val approvalNotification = if (cryptoCurrencyStatus.value.yieldSupplyStatus?.isAllowedToSpend != true) {
            NotificationUM.Error(
                title = resourceReference(R.string.yield_module_approve_needed_notification_title),
                subtitle = resourceReference(R.string.yield_module_approve_needed_notification_description),
                iconResId = R.drawable.ic_alert_triangle_20,
                buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                    text = resourceReference(R.string.yield_module_approve_needed_notification_cta),
                    onClick = params.callback::onApprove,
                ),
            )
        } else {
            null
        }
        val notSuppliedNotification = getNotSuppliedNotification(cryptoCurrencyStatus)
        return listOfNotNull(
            approvalNotification,
            notSuppliedNotification,
        ).toPersistentList()
    }

    private fun getNotSuppliedNotification(cryptoCurrencyStatus: CryptoCurrencyStatus): NotificationUM? {
        val value = cryptoCurrencyStatus.value
        val isActive = value.yieldSupplyStatus?.isActive == true
        val effectiveProtocolBalance = value.yieldSupplyStatus?.effectiveProtocolBalance ?: null
        val amount = value.amount

        if (!isActive || effectiveProtocolBalance == null || amount == null) return null

        val notDepositedAmount = amount.minus(effectiveProtocolBalance)
        return if (notDepositedAmount > BigDecimal.ZERO) {
            val formattedAmount =
                notDepositedAmount.format { crypto(symbol = "", decimals = cryptoCurrencyStatus.currency.decimals) }
            analyticsHandler.send(
                YieldSupplyAnalytics.NoticeAmountNotDeposited(
                    token = cryptoCurrency.symbol,
                    blockchain = cryptoCurrency.network.name,
                ),
            )
            NotificationUM.Info.YieldSupplyNotAllAmountSupplied(
                formattedAmount = formattedAmount,
                symbol = cryptoCurrency.symbol,
            )
        } else {
            null
        }
    }

    private fun loadMinAmount() {
        modelScope.launch(dispatchers.default) {
            yieldSupplyMinAmountUseCase(
                params.userWallet,
                cryptoCurrencyStatusFlow.value,
            ).onRight { minAmount ->
                uiState.update(
                    YieldSupplyActiveMinAmountTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value,
                        appCurrency = appCurrency,
                        minAmount = minAmount,
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
                userWallet = params.userWallet,
                cryptoCurrencyStatus = cryptoStatus,
            ).getOrNull()

            val maxFee = yieldSupplyGetMaxFeeUseCase(
                userWallet = params.userWallet,
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