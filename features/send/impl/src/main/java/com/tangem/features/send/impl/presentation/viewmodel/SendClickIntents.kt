package com.tangem.features.send.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal interface SendClickIntents : AmountScreenClickIntents {

    fun popBackStack()

    fun onBackClick()

    fun onCloseClick()

    fun onNextClick(isFromEdit: Boolean = false)

    fun onPrevClick()

    fun onQrCodeScanClick()

    fun onFailedTxEmailClick(errorMessage: String)

    fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency)

    // region Recipient
    fun onRecipientAddressValueChange(value: String, type: EnterAddressSource? = null)

    fun onRecipientMemoValueChange(value: String, isValuePasted: Boolean = false)
    // endregion

    // region Fee
    fun feeReload()

    fun onFeeSelectorClick(feeType: FeeType)

    fun onCustomFeeValueChange(index: Int, value: String)

    fun onReadMoreClick()
    // endregion

    // region Send
    fun onSendClick()

    fun showAmount()

    fun showRecipient()

    fun showFee()

    fun showSend()

    fun onExploreClick()

    fun onShareClick()

    fun onAmountReduceClick(
        reduceAmountBy: BigDecimal? = null,
        reduceAmountByDiff: BigDecimal? = reduceAmountBy,
        reduceAmountTo: BigDecimal? = null,
        clazz: Class<out SendNotification>,
    )

    fun onNotificationCancel(clazz: Class<out SendNotification>)
    // endregion
}
