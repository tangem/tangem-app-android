package com.tangem.features.walletconnect.utils

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import com.tangem.features.walletconnect.impl.R
import javax.inject.Inject

internal class WcNotificationsFactory @Inject constructor() {

    fun createFeeNotifications(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeSelectorUM: FeeSelectorUM?,
        onFeeReload: () -> Unit,
    ): NotificationUM.Info? {
        return when (feeSelectorUM) {
            is FeeSelectorUM.Content -> createFeeExceedsBalance(cryptoCurrencyStatus, feeSelectorUM)
            is FeeSelectorUM.Error -> createFeeErrorNotification(onFeeReload)
            FeeSelectorUM.Loading, null -> null
        }
    }

    private fun createFeeExceedsBalance(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeSelectorUM: FeeSelectorUM?,
    ): NotificationUM.Info? {
        // TODO: [REDACTED_TASK_KEY] localization
        return NotificationUM.Info(
            title = stringReference("Insufficient ${cryptoCurrencyStatus.currency.name}"),
            subtitle = stringReference("Top up your balance to cover the network fee"),
        ).takeIf { isFeeExceedsBalance(cryptoCurrencyStatus = cryptoCurrencyStatus, feeSelectorUM = feeSelectorUM) }
    }

    private fun createFeeErrorNotification(onFeeReload: () -> Unit): NotificationUM.Info {
        return NotificationUM.Info(
            title = resourceReference(R.string.send_fee_unreachable_error_title),
            subtitle = resourceReference(R.string.send_fee_unreachable_error_text),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.warning_button_refresh),
                onClick = onFeeReload,
            ),
        )
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