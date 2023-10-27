package com.tangem.tap.common.chat

import android.content.Context
import com.tangem.datasource.config.models.ChatConfig
import com.tangem.datasource.config.models.SprinklrConfig
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.tap.common.chat.opener.implementation.SprinklrChatOpener
import java.io.File

class ChatManager(private val foregroundActivityObserver: ForegroundActivityObserver) {
    private val openers = mutableMapOf<ChatConfig, ChatOpener>()

    fun open(config: ChatConfig, createLogsFile: (Context) -> File?, createFeedbackFile: (Context) -> File?) {
        val opener = openers.getOrPut(config) {
            when (config) {
                is SprinklrConfig -> SprinklrChatOpener(config, foregroundActivityObserver)
            }
        }

        opener.open(createFeedbackFile, createLogsFile)
    }
}