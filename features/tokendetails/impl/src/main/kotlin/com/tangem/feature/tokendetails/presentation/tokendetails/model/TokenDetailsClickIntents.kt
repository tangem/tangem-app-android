package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.tokens.model.CryptoCurrency
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

    fun onDismissDialog()

    fun onHideClick()

    fun onHideConfirmed()

    fun onRefreshSwipe(isRefreshing: Boolean)

    fun onBuyCoinClick(cryptoCurrency: CryptoCurrency)

    fun onReloadClick()

    fun onExploreClick()

    fun onTransactionClick(txHash: String)

    fun onAddressTypeSelected(addressModel: AddressModel)

    fun onDismissBottomSheet()

    fun onCloseRentInfoNotification()

    fun onExpressTransactionClick(txId: String)

    fun onGoToProviderClick(url: String)

    fun onSwapPromoDismiss(promoId: PromoId)

    fun onSwapPromoClick(promoId: PromoId)

    fun onGenerateExtendedKey()

    fun onCopyAddress(): TextReference?

    fun onAssociateClick()

    fun onRetryIncompleteTransactionClick()

    fun onDismissIncompleteTransactionClick()

    fun onConfirmDismissIncompleteTransactionClick()

    fun onStakeBannerClick()

    fun onBalanceSelect(config: TokenBalanceSegmentedButtonConfig)

    fun onGoToRefundedTokenClick(cryptoCurrency: CryptoCurrency)

    fun onOpenUrlClick(url: String)

    fun onConfirmDisposeExpressStatus()

    fun onDisposeExpressStatus()
}