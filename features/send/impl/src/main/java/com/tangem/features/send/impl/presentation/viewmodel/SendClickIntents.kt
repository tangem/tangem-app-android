package com.tangem.features.send.impl.presentation.viewmodel

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.impl.presentation.state.fee.FeeType

interface SendClickIntents {

    fun popBackStack()

    fun onBackClick()

    fun onNextClick()

    fun onPrevClick()

    fun onQrCodeScanClick()

    fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency)

    // region Amount
    fun onAmountValueChange(value: String)

    fun onCurrencyChangeClick(isFiat: Boolean)

    fun onMaxValueClick()
    // endregion

    // region Recipient
    fun onRecipientAddressValueChange(value: String)

    fun onRecipientMemoValueChange(value: String)
    // endregion

    // region Fee
    fun onFeeSelectorClick(feeType: FeeType)

    fun onCustomFeeValueChange(index: Int, value: String)

    fun onSubtractSelect(value: Boolean)
    // endregion

    // region Send
    fun onSendClick()

    fun showAmount()

    fun showRecipient()

    fun showFee()

    fun onExploreClick(txUrl: String)
    // endregion
}