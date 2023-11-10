package com.tangem.features.send.impl.presentation.viewmodel

interface SendClickIntents {

    fun onBackClick()

    fun onNextClick()

    fun onPrevClick()

    fun onQrCodeScanClick()

    // region Amount
    fun onAmountValueChange(value: String)

    fun onCurrencyChangeClick(isFiat: Boolean)

    fun onMaxValueClick()
    // endregion

    // region Recipient
    fun onRecipientAddressValueChange(value: String)

    fun onRecipientMemoValueChange(value: String)
    // endregion
}