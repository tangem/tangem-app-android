package com.tangem.tap.domain.moduleMessage.domain

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.DomainModuleError
import com.tangem.domain.DomainModuleMessage
import com.tangem.tap.domain.moduleMessage.ConvertedMessage
import com.tangem.tap.domain.moduleMessage.domain.converter.AddCustomTokenErrorConverter

/**
[REDACTED_AUTHOR]
 */
class DomainMessageConverter(
    private val context: Context,
) : ModuleMessageConverter<DomainModuleMessage, ConvertedMessage?> {

    override fun convert(message: DomainModuleMessage): ConvertedMessage? {
        return when (message) {
            is DomainModuleError -> DomainErrorConverter(context).convert(message)
            else -> null
        }
    }
}

class DomainErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<DomainModuleError, ConvertedMessage?> {

    override fun convert(message: DomainModuleError): ConvertedMessage? = when (message) {
        is AddCustomTokenError -> AddCustomTokenErrorConverter(context).convert(message)
        else -> null
    }
}