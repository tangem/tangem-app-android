package com.tangem.feature.swap.converters

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.feature.swap.models.SwapAlertUM
import com.tangem.utils.converter.Converter

/** The alert for a failed transfer out of the collectibles vault; null when the user cancelled it. */
internal class CollectiblesTransferErrorConverter(
    private val onDismiss: () -> Unit,
) : Converter<CollectiblesAccountError, DialogMessage?> {

    override fun convert(value: CollectiblesAccountError): DialogMessage? {
        val messageId = when (value) {
            CollectiblesAccountError.Cancelled -> return null
            is CollectiblesAccountError.MaybeSent -> R.string.collectibles_withdraw_transfer_error_maybe_sent
            is CollectiblesAccountError.Vault -> when (value.error) {
                GachaVaultError.SponsorUnavailable -> R.string.gasless_solana_error_unavailable
                GachaVaultError.SponsorRateLimited -> R.string.collectibles_withdraw_transfer_error_rate_limited
                GachaVaultError.TransactionExpired -> R.string.collectibles_withdraw_transfer_error_expired
                else -> null
            }
            else -> null
        }

        return if (messageId == null) {
            SwapAlertUM.genericError(onConfirmClick = onDismiss)
        } else {
            SwapAlertUM.errorAlert(message = resourceReference(messageId), onConfirmClick = onDismiss)
        }
    }
}