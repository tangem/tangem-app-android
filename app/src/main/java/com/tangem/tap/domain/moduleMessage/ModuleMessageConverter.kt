package com.tangem.tap.domain.moduleMessage

import android.content.Context
import com.tangem.common.module.ModuleMessage
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.domain.DomainModuleMessage
import com.tangem.tap.domain.moduleMessage.domain.DomainMessageConverter

class ModuleMessageConverter(
    private val context: Context,
) : ModuleMessageConverter<ModuleMessage, ConvertedMessage> {

    override fun convert(message: ModuleMessage): ConvertedMessage {
        val convertedMessage = when (message) {
            is DomainModuleMessage -> DomainMessageConverter(context).convert(message)
            else -> null
        }
        return convertedMessage ?: convertUnknownMessage(message)
    }

    private fun convertUnknownMessage(message: ModuleMessage): ConvertedMessage {
        return ConvertedStringMessage("Unknown message: ${message::class.java.simpleName}")
    }
}