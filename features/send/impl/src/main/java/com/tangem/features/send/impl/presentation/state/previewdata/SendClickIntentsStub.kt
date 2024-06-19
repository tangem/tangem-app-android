package com.tangem.features.send.impl.presentation.state.previewdata

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal object SendClickIntentsStub : SendClickIntents {
    override fun popBackStack() {}

    override fun onBackClick() {}

    override fun onCloseClick() {}

    override fun onNextClick(isFromEdit: Boolean) {}

    override fun onPrevClick() {}

    override fun onQrCodeScanClick() {}

    override fun onFailedTxEmailClick(errorMessage: String) {}

    override fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency) {}

    override fun onAmountValueChange(value: String) {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}
    override fun onAmountNext() {}

    override fun onMaxValueClick() {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onRecipientAddressValueChange(value: String, type: EnterAddressSource?) {}

    override fun onRecipientMemoValueChange(value: String, isValuePasted: Boolean) {}

    override fun feeReload() {}

    override fun onFeeSelectorClick(feeType: FeeType) {}

    override fun onCustomFeeValueChange(index: Int, value: String) {}

    override fun onReadMoreClick() {}

    override fun onSendClick() {}

    override fun showAmount() {}

    override fun showRecipient() {}

    override fun showFee() {}

    override fun showSend() {}

    override fun onExploreClick() {}

    override fun onShareClick() {}

    override fun onAmountReduceClick(
        reduceAmountBy: BigDecimal?,
        reduceAmountByDiff: BigDecimal?,
        reduceAmountTo: BigDecimal?,
        clazz: Class<out SendNotification>,
    ) {
    }

    override fun onNotificationCancel(clazz: Class<out SendNotification>) {}
}
