package com.tangem.tap.domain.moduleMessage.saltPay

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.tap.domain.moduleMessage.ConvertedMessage
import com.tangem.tap.domain.moduleMessage.saltPay.converter.SaltPayRegistrationErrorConverter
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayModuleError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayModuleMessage
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError

/**
[REDACTED_AUTHOR]
 */
class SaltPayMessageConverter(
    private val context: Context,
) : ModuleMessageConverter<SaltPayModuleMessage, ConvertedMessage?> {

    override fun convert(message: SaltPayModuleMessage): ConvertedMessage? {
        return when (message) {
            is SaltPayModuleError -> SaltPayErrorConverter(context).convert(message)
            else -> null
        }
    }
}

class SaltPayErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<SaltPayModuleError, ConvertedMessage?> {

    override fun convert(message: SaltPayModuleError): ConvertedMessage? = when (message) {
        is SaltPayRegistrationError -> SaltPayRegistrationErrorConverter(context).convert(message)
        else -> null
    }
}
