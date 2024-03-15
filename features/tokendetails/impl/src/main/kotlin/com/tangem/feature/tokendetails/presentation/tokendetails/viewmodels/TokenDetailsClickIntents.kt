package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.domain.tokens.model.CryptoCurrency

@Suppress("TooManyFunctions")
interface TokenDetailsClickIntents {

    fun onBackClick()

    fun onSendClick()

    fun onReceiveClick()

    fun onSellClick()

    fun onSwapClick()

    fun onDismissDialog()

    fun onHideClick()

    fun onHideConfirmed()

    fun onRefreshSwipe()

    fun onBuyClick()

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
}