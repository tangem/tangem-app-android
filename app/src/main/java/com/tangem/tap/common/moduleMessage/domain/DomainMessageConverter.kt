package com.tangem.tap.common.moduleMessage.domain

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.domain.DomainError
import com.tangem.domain.DomainModuleMessage

/**
[REDACTED_AUTHOR]
 */
class DomainMessageConverter(
    private val context: Context
) : ModuleMessageConverter<DomainModuleMessage, String?> {
    override fun convert(message: DomainModuleMessage): String? {
        return when (message) {
            is DomainError -> DomainErrorConverter(context).convert(message)
            else -> null
        }
    }
}