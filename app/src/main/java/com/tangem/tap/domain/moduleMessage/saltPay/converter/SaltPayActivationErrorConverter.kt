package com.tangem.tap.domain.moduleMessage.saltPay.converter

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.tap.domain.moduleMessage.ConvertedDialogMessage
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 09.10.2022.
 */
internal class SaltPayActivationErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<SaltPayActivationError, ConvertedDialogMessage?> {

    override fun convert(message: SaltPayActivationError): ConvertedDialogMessage? {
        val saltPayError = message as? SaltPayActivationError ?: throw UnsupportedOperationException()

        val dialogMessage = when (saltPayError) {
            SaltPayActivationError.NoGas -> {
                ConvertedDialogMessage(
                    title = context.getString(R.string.saltpay_error_no_gas_title),
                    message = context.getString(R.string.saltpay_error_no_gas_message),
                )
            }
            SaltPayActivationError.EmptyBackupCardScanned -> {
                ConvertedDialogMessage(
                    title = context.getString(R.string.saltpay_error_empty_backup_title),
                    message = context.getString(R.string.saltpay_error_empty_backup_message),
                )
            }
            SaltPayActivationError.WeakPin -> {
                ConvertedDialogMessage(
                    title = context.getString(R.string.saltpay_error_pin_weak_title),
                    message = context.getString(R.string.saltpay_error_pin_weak_message),
                )
            }
            else -> {
                val errorMessage = if (saltPayError.message == SaltPayActivationError.EMPTY_MESSAGE) {
                    saltPayError::class.java.simpleName
                } else {
                    saltPayError.message
                }
                ConvertedDialogMessage(
                    title = context.getString(R.string.common_error),
                    message = errorMessage,
                )
            }
        }

        return dialogMessage
    }
}
