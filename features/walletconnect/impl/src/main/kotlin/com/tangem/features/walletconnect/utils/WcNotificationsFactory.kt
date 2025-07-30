package com.tangem.features.walletconnect.utils

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import javax.inject.Inject

internal class WcNotificationsFactory @Inject constructor() {

    fun createFeeExceedsBalance(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeSelectorUM: FeeSelectorUM?,
    ): NotificationUM.Info? {
        // TODO: [REDACTED_TASK_KEY] localization
        return NotificationUM.Info(
            title = stringReference("Insufficient ${cryptoCurrencyStatus.currency.name}"),
            subtitle = stringReference("Top up your balance to cover the network fee"),
        ).takeIf { isFeeExceedsBalance(cryptoCurrencyStatus = cryptoCurrencyStatus, feeSelectorUM = feeSelectorUM) }
    }

    private fun isFeeExceedsBalance(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeSelectorUM: FeeSelectorUM?,
    ): Boolean {
        val feeSelectorContent = feeSelectorUM as? FeeSelectorUM.Content ?: return false
        val lowestFee = when (val fees = feeSelectorContent.fees) {
            is TransactionFee.Choosable -> fees.minimum
            is TransactionFee.Single -> fees.normal
        }

        return FeeCalculationUtils.checkExceedBalance(
            feeBalance = cryptoCurrencyStatus.value.amount,
            feeAmount = lowestFee.amount.value,
        )
    }
}