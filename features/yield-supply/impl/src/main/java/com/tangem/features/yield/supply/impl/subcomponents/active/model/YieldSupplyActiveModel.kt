package com.tangem.features.yield.supply.impl.subcomponents.active.model

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetProtocolBalanceUseCase
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyActiveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyGetProtocolBalanceUseCase: YieldSupplyGetProtocolBalanceUseCase,
) : Model() {

    private val params: YieldSupplyActiveComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow
    private val cryptoCurrency = cryptoCurrencyStatusFlow.value.currency

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
                notificationUM = null,
            ),
        )

    init {
        subscribeOnCurrencyUpdates()

        modelScope.launch(dispatchers.default) {
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
            val protocolBalance = yieldSupplyGetProtocolBalanceUseCase(
                userWalletId = params.userWallet.walletId,
                cryptoCurrency = cryptoCurrency,
            ).getOrNull()

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

            uiState.update {
                it.copy(
                    notificationUM = approvalNotification,
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

    private companion object {
        const val AAVEV3_PREFIX = "a"
    }
}