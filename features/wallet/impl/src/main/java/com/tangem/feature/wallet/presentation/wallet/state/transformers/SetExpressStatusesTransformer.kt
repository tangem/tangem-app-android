package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.SingleWalletOnrampTransactionConverter
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

internal class SetExpressStatusesTransformer(
    userWalletId: UserWalletId,
    private val onrampTxs: List<OnrampTransaction>,
    private val clickIntents: WalletClickIntents,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletStateTransformer(userWalletId) {
    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> {
                val expressTxs = SingleWalletOnrampTransactionConverter(
                    clickIntents = clickIntents,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    appCurrency = appCurrency,
                    analyticsEventHandler = analyticsEventHandler,
                ).convertList(onrampTxs).toPersistentList()

                val expressTxsToDisplay = expressTxs.filterNot {
                    it.activeStatus.isHidden
                }.toPersistentList()

                val expressBottomSheet = prevState.bottomSheetConfig?.content as? ExpressStatusBottomSheetConfig
                val currentTx = expressTxs.firstOrNull { it.info.txId == expressBottomSheet?.value?.info?.txId }

                prevState.copy(
                    expressTxs = expressTxs,
                    expressTxsToDisplay = expressTxsToDisplay,
                    bottomSheetConfig = prevState.bottomSheetConfig?.updateStateWithExpressStatusBottomSheet(currentTx),
                )
            }
            is WalletState.SingleCurrency.Locked -> {
                Timber.w("Impossible to load express statuses for locked wallet")
                prevState
            }
            is WalletState.Visa -> {
                Timber.w("Impossible to load express statuses for visa wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.w("Impossible to load express statuses for multi-currency wallet")
                prevState
            }
        }
    }

    private fun TangemBottomSheetConfig.updateStateWithExpressStatusBottomSheet(
        expressState: ExpressTransactionStateUM?,
    ): TangemBottomSheetConfig {
        val currentConfig = this.content as? ExpressStatusBottomSheetConfig ?: return this
        if (expressState == null) return this
        return copy(
            content = if (currentConfig.value != expressState) {
                ExpressStatusBottomSheetConfig(expressState)
            } else {
                currentConfig
            },
        )
    }
}