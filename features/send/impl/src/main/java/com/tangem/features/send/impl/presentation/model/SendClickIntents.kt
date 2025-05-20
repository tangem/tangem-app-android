package com.tangem.features.send.impl.presentation.model

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
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

    fun onTokenDetailsClick(currency: CryptoCurrency)

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

    fun onShareClick(txUrl: String)

    fun onAmountReduceByClick(
        reduceAmountBy: BigDecimal,
        reduceAmountByDiff: BigDecimal,
        notification: Class<out NotificationUM>,
    )

    fun onAmountReduceToClick(reduceAmountTo: BigDecimal, notification: Class<out NotificationUM>)

    fun onNotificationCancel(clazz: Class<out NotificationUM>)
    // endregion
}