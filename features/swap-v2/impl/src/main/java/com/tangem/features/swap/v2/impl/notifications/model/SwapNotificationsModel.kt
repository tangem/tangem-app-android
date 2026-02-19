package com.tangem.features.swap.v2.impl.notifications.model

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.features.swap.v2.impl.notifications.DefaultSwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent.Params.SwapNotificationData
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateListener
import com.tangem.features.swap.v2.impl.notifications.entity.SwapNotificationUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class SwapNotificationsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val swapNotificationsUpdateListener: SwapNotificationsUpdateListener,
    private val swapNotificationsUpdateTrigger: DefaultSwapNotificationsUpdateTrigger,
    private val swapAmountUpdateTrigger: SwapAmountUpdateTrigger,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SwapNotificationsComponent.Params = paramsContainer.require()

    private var notificationData = params.swapNotificationData

    val uiState: StateFlow<ImmutableList<NotificationUM>>
        field = MutableStateFlow<ImmutableList<NotificationUM>>(persistentListOf())

    init {
        subscribeToNotificationUpdateTrigger()
        modelScope.launch {
            buildNotifications()
        }
    }

    private fun subscribeToNotificationUpdateTrigger() {
        swapNotificationsUpdateListener.updateTriggerFlow
            .onEach { updateState(it) }
            .launchIn(modelScope)
    }

    private suspend fun updateState(data: SwapNotificationData) {
        notificationData = data
        buildNotifications()
    }

    private suspend fun buildNotifications() {
        val notifications = buildList {
            addExpressErrorNotification()
            addDestinationTagRequiredNotification()
        }

        swapNotificationsUpdateTrigger.callbackHasError(notifications.isNotEmpty())
        uiState.value = notifications.toImmutableList()
    }

    private suspend fun MutableList<NotificationUM>.addDestinationTagRequiredNotification() {
        val toCryptoCurrencyStatus = notificationData.toCryptoCurrencyStatus ?: return
        val userWalletId = notificationData.userWalletId ?: return
        val destinationAddress = notificationData.destinationAddress
        if (destinationAddress.isEmpty()) return

        val validationError = validateTransactionUseCase(
            amount = BigDecimal.ZERO.convertToSdkAmount(toCryptoCurrencyStatus),
            fee = null,
            memo = notificationData.memo,
            destination = destinationAddress,
            userWalletId = userWalletId,
            network = toCryptoCurrencyStatus.currency.network,
        ).leftOrNull()

        if (validationError is BlockchainSdkError.DestinationTagRequired) {
            add(NotificationUM.Error.DestinationTagRequired)
        }
    }

    fun MutableList<NotificationUM>.addExpressErrorNotification() {
        val expressError = notificationData.expressError ?: return
        val fromCryptoCurrency = notificationData.fromCryptoCurrency ?: return

        val errorNotification = when (expressError) {
            is ExpressError.AmountError.TooSmallError -> SwapNotificationUM.Error.MinimalAmountError(
                expressError.amount.format {
                    crypto(
                        symbol = fromCryptoCurrency.symbol,
                        decimals = fromCryptoCurrency.decimals,
                    )
                },
            )
            is ExpressError.AmountError.TooBigError -> SwapNotificationUM.Error.MaximumAmountError(
                expressError.amount.format {
                    crypto(
                        symbol = fromCryptoCurrency.symbol,
                        decimals = fromCryptoCurrency.decimals,
                    )
                },
            )
            else -> SwapNotificationUM.Warning.ExpressGeneralError(
                expressError = expressError,
                onConfirmClick = {
                    modelScope.launch {
                        swapAmountUpdateTrigger.triggerQuoteReload()
                    }
                },
            )
        }

        add(errorNotification)
    }
}