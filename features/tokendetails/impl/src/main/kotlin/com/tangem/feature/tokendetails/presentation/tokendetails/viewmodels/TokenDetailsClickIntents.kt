package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason

@Suppress("TooManyFunctions")
interface TokenDetailsClickIntents {

    fun onBackClick()

    fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSendClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSwapClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSellClick(unavailabilityReason: ScenarioUnavailabilityReason)

    fun onDismissDialog()

    fun onHideClick()

    fun onHideConfirmed()

    fun onRefreshSwipe()

    fun onBuyCoinClick(cryptoCurrency: CryptoCurrency)

    fun onReloadClick()

    fun onExploreClick()

    fun onTransactionClick(txHash: String)

    fun onAddressTypeSelected(addressModel: AddressModel)

    fun onDismissBottomSheet()

    fun onCloseRentInfoNotification()

    fun onSwapTransactionClick(txId: String)

    fun onGoToProviderClick(url: String)

    fun onSwapPromoDismiss()

    fun onSwapPromoClick()

    fun onGenerateExtendedKey()

    fun onCopyAddress(): TextReference?

    fun onAssociateClick()

    fun onGoToRefundedTokenClick(cryptoCurrency: CryptoCurrency)
}
