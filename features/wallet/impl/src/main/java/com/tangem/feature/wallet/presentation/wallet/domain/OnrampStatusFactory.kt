package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.swaptx.ExpressAnalyticsStatus
import com.tangem.domain.onramp.GetOnrampStatusUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.OnrampUpdateTransactionStatusUseCase
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.OnrampStatus.Status.*
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
internal class OnrampStatusFactory @Inject constructor(
    private val stateHolder: WalletStateController,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val getOnrampStatusUseCase: GetOnrampStatusUseCase,
    private val onrampUpdateTransactionStatusUseCase: OnrampUpdateTransactionStatusUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun removeTransactionOnBottomSheetClosed() {
        val state = stateHolder.getSelectedWallet()
        val bottomSheetConfig = state.bottomSheetConfig?.content as? ExpressStatusBottomSheetConfig ?: return
        val selectedTx = bottomSheetConfig.value as? ExpressTransactionStateUM.OnrampUM ?: return

        if (selectedTx.activeStatus.isTerminal) {
            onrampRemoveTransactionUseCase(externalTxId = selectedTx.info.txExternalId)
        }
    }

    suspend fun updateOnrmapTransactionStatuses() = withContext(dispatchers.io) {
        val singleWalletState = stateHolder.getSelectedWallet() as? WalletState.SingleCurrency.Content
            ?: return@withContext

        singleWalletState.expressTxs.map { tx ->
            async {
                if (tx is ExpressTransactionStateUM.OnrampUM) {
                    updateOnrampTxStatus(tx)
                }
            }
        }.awaitAll()
    }

    private suspend fun updateOnrampTxStatus(onrampTx: ExpressTransactionStateUM.OnrampUM) {
        if (!onrampTx.activeStatus.isTerminal) {
            getOnrampStatusUseCase(onrampTx.info.txId).fold(
                ifLeft = {
                    Timber.e("Couldn't update onramp status. $it")
                },
                ifRight = { statusModel ->
                    val externalTxId = statusModel.externalTxId
                    val status = toAnalyticStatus(statusModel.status) ?: return

                    if (statusModel.status != onrampTx.activeStatus) {
                        analyticsEventHandler.send(
                            TokenOnrampAnalyticsEvent.OnrampStatusChanged(
                                tokenSymbol = onrampTx.info.toAmountSymbol,
                                status = status.name,
                                provider = onrampTx.providerName,
                                fiatCurrency = onrampTx.fromCurrencyCode,
                            ),
                        )
                        onrampUpdateTransactionStatusUseCase(externalTxId = externalTxId, statusModel.status)
                    }
                },
            )
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
}
