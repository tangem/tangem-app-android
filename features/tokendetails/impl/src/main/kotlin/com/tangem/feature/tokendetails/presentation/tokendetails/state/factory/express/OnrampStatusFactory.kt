package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.swaptx.ExpressAnalyticsStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.GetOnrampStatusUseCase
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.OnrampUpdateTransactionStatusUseCase
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.OnrampStatus.Status.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsOnrampTransactionStateConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Suppress("LongParameterList")
internal class OnrampStatusFactory @AssistedInject constructor(
    private val getOnrampTransactionsUseCase: GetOnrampTransactionsUseCase,
    private val getOnrampStatusUseCase: GetOnrampStatusUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val onrampUpdateTransactionStatusUseCase: OnrampUpdateTransactionStatusUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    @Assisted private val currentStateProvider: Provider<TokenDetailsState>,
    @Assisted private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    @Assisted private val appCurrencyProvider: Provider<AppCurrency>,
    @Assisted private val clickIntents: TokenDetailsClickIntents,
    @Assisted private val cryptoCurrency: CryptoCurrency,
    @Assisted private val userWalletId: UserWalletId,
) {

    private val onrampTransactionStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        TokenDetailsOnrampTransactionStateConverter(
            clickIntents = clickIntents,
            cryptoCurrency = cryptoCurrency,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    operator fun invoke(): Flow<List<ExpressTransactionStateUM.OnrampUM>> {
        return getOnrampTransactionsUseCase(
            userWalletId = userWalletId,
            cryptoCurrencyId = cryptoCurrency.id,
        ).map { maybeTransaction ->
            maybeTransaction.fold(
                ifRight = { onrampTxs ->
                    val transactions = onrampTransactionStateConverter.convertList(onrampTxs)
                    transactions.clearHiddenTerminal()
                    transactions
                },
                ifLeft = { persistentListOf() },
            )
        }
    }

    suspend fun removeTransactionOnBottomSheetClosed() {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig?.content as? ExpressStatusBottomSheetConfig ?: return
        val selectedTx = bottomSheetConfig.value as? ExpressTransactionStateUM.OnrampUM ?: return

        if (selectedTx.activeStatus.isTerminal) {
            onrampRemoveTransactionUseCase(externalTxId = selectedTx.info.txExternalId)
        }
    }

    suspend fun updateOnrmapTxStatus(onrampTx: ExpressTransactionStateUM.OnrampUM): ExpressTransactionStateUM.OnrampUM {
        return if (onrampTx.activeStatus.isTerminal) {
            onrampTx
        } else {
            getOnrampStatusUseCase(onrampTx.info.txId).fold(
                ifLeft = {
                    Timber.e("Couldn't update onramp status. $it")
                    onrampTx
                },
                ifRight = { statusModel ->
                    sendStatusUpdateAnalytics(onrampTx, statusModel)
                    onrampTx.copy(activeStatus = statusModel.status)
                },
            )
        }
    }

    private suspend fun List<ExpressTransactionStateUM.OnrampUM>.clearHiddenTerminal() {
        this.filter { it.activeStatus.isHidden && it.activeStatus.isTerminal }
            .forEach { onrampRemoveTransactionUseCase(externalTxId = it.info.txExternalId) }
    }

    private suspend fun sendStatusUpdateAnalytics(
        onrampTx: ExpressTransactionStateUM.OnrampUM,
        statusModel: OnrampStatus,
    ) {
        val externalTxId = statusModel.externalTxId
        val status = toAnalyticStatus(statusModel.status) ?: return

        if (statusModel.status != onrampTx.activeStatus) {
            analyticsEventHandler.send(
                TokenOnrampAnalyticsEvent.OnrampStatusChanged(
                    tokenSymbol = cryptoCurrency.symbol,
                    status = status.name,
                    provider = onrampTx.providerName,
                    fiatCurrency = onrampTx.fromCurrencyCode,
                ),
            )
            onrampUpdateTransactionStatusUseCase(externalTxId = externalTxId, statusModel.status)
        }
    }

    private fun toAnalyticStatus(status: OnrampStatus.Status?): ExpressAnalyticsStatus? {
        return when (status) {
            Expired,
            Paused,
            -> ExpressAnalyticsStatus.Cancelled
            Created,
            WaitingForPayment,
            PaymentProcessing,
            Paid,
            Sending,
            -> ExpressAnalyticsStatus.InProgress
            Verifying -> ExpressAnalyticsStatus.KYC
            Failed -> ExpressAnalyticsStatus.Fail
            Finished -> ExpressAnalyticsStatus.Done
            null -> null
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            currentStateProvider: Provider<TokenDetailsState>,
            cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
            appCurrencyProvider: Provider<AppCurrency>,
            clickIntents: TokenDetailsClickIntents,
            cryptoCurrency: CryptoCurrency,
            userWalletId: UserWalletId,
        ): OnrampStatusFactory
    }
}
