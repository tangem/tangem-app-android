package com.tangem.tap.domain.moduleMessage.saltPay.converter

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.tap.domain.moduleMessage.ConvertedDialogMessage
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
internal class SaltPayRegistrationErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<SaltPayRegistrationError, ConvertedDialogMessage?> {

    override fun convert(message: SaltPayRegistrationError): ConvertedDialogMessage? {
        val saltPayError = (message as? SaltPayRegistrationError) ?: throw UnsupportedOperationException()

        val dialogMessage = when (saltPayError) {
            SaltPayRegistrationError.NoGas -> {
                ConvertedDialogMessage(
                    title = context.getString(R.string.saltpay_error_no_gas_title),
                    message = context.getString(R.string.saltpay_error_no_gas_message),
                )
            }
            SaltPayRegistrationError.EmptyBackupCardScanned -> {
                ConvertedDialogMessage(
                    title = context.getString(R.string.saltpay_error_empty_backup_title),
                    message = context.getString(R.string.saltpay_error_empty_backup_message),
                )
            }
            SaltPayRegistrationError.WeakPin -> {
                ConvertedDialogMessage(
                    title = context.getString(R.string.saltpay_error_pin_weak_title),
                    message = context.getString(R.string.saltpay_error_pin_weak_message),
                )
            }
            else -> ConvertedDialogMessage(
                title = context.getString(R.string.common_error),
                message = saltPayError.message,
            )
        }

        return dialogMessage
    }
}
