package com.tangem.tap.common.moduleMessage

import android.content.Context
import com.tangem.common.module.ModuleMessage
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.domain.DomainModuleMessage
import com.tangem.tap.common.moduleMessage.domain.DomainMessageConverter

class ModuleMessageConverter(
    private val context: Context
) : ModuleMessageConverter<ModuleMessage, String> {

    override fun convert(message: ModuleMessage): String {
        val convertedMessage = when (message) {
            is DomainModuleMessage -> DomainMessageConverter(context).convert(message)
            else -> null
        }
        return convertedMessage ?: convertUnknownMessage(message)
    }

    private fun convertUnknownMessage(message: ModuleMessage): String {
        return "Unknown message: ${message::class.java.simpleName}"
    }
}