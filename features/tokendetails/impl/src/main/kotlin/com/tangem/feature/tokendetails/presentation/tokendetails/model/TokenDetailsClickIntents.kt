package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig

@Suppress("TooManyFunctions")
interface TokenDetailsClickIntents {

    fun onBackClick()

    fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onStakeClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSendClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSwapClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSellClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onHideClick()

    fun onHideConfirmed()

    fun onRefreshSwipe(isRefreshing: Boolean)

    fun onBuyCoinClick(cryptoCurrency: CryptoCurrency)

    fun onReloadClick()

    fun onExploreClick()

    fun onTransactionClick(txHash: String)

    fun onAddressTypeSelected(addressModel: AddressModel)

    fun onCloseRentInfoNotification()

    fun onSwapPromoDismiss(promoId: PromoId)

    fun onSwapPromoClick(promoId: PromoId)

    fun onGenerateExtendedKey()

    fun onCopyAddress(): TextReference?

    fun onAssociateClick()

    fun onRetryIncompleteTransactionClick()

    fun onOpenTrustlineClick()

    fun onDismissIncompleteTransactionClick()

    fun onConfirmDismissIncompleteTransactionClick()

    fun onStakeBannerClick()

    fun onBalanceSelect(config: TokenBalanceSegmentedButtonConfig)

    fun onYieldInfoClick()
}

interface ExpressTransactionsClickIntents {

    fun onExpressTransactionClick(txId: String)

    fun onGoToProviderClick(url: String)

    fun onGoToRefundedTokenClick(cryptoCurrency: CryptoCurrency)

    fun onOpenUrlClick(url: String)

    fun onConfirmDisposeExpressStatus()

    fun onDisposeExpressStatus()

    fun onDismissBottomSheet()

    fun onDismissDialog()
}

@Suppress("TooManyFunctions")
internal class EmptyTokenDetailsClickIntents : TokenDetailsClickIntents {
    override fun onRefreshSwipe(isRefreshing: Boolean) { /* no op */ }

    override fun onBackClick() { /* no op */ }

    override fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason) { /* no op */ }

    override fun onBuyCoinClick(cryptoCurrency: CryptoCurrency) { /* no op */ }

    override fun onStakeBannerClick() { /* no op */ }

    override fun onReloadClick() { /* no op */ }

    override fun onSendClick(unavailabilityReason: ScenarioUnavailabilityReason) { /* no op */ }

    override fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason) { /* no op */ }

    override fun onStakeClick(unavailabilityReason: ScenarioUnavailabilityReason) { /* no op */ }

    override fun onGenerateExtendedKey() { /* no op */ }

    override fun onSellClick(unavailabilityReason: ScenarioUnavailabilityReason) { /* no op */ }

    override fun onSwapClick(unavailabilityReason: ScenarioUnavailabilityReason) { /* no op */ }

    override fun onHideClick() { /* no op */ }

    override fun onHideConfirmed() { /* no op */ }

    override fun onExploreClick() { /* no op */ }

    override fun onAddressTypeSelected(addressModel: AddressModel) { /* no op */ }

    override fun onTransactionClick(txHash: String) { /* no op */ }

    override fun onCloseRentInfoNotification() { /* no op */ }

    override fun onSwapPromoDismiss(promoId: PromoId) { /* no op */ }

    override fun onSwapPromoClick(promoId: PromoId) { /* no op */ }

    override fun onRetryIncompleteTransactionClick() { /* no op */ }

    override fun onOpenTrustlineClick() { /* no op */ }

    override fun onDismissIncompleteTransactionClick() { /* no op */ }

    override fun onConfirmDismissIncompleteTransactionClick() { /* no op */ }

    override fun onAssociateClick() { /* no op */ }

    override fun onBalanceSelect(config: TokenBalanceSegmentedButtonConfig) { /* no op */ }

    override fun onYieldInfoClick() { /* no op */ }

    override fun onCopyAddress(): TextReference? {
        /* no op */
        return null
    }
}