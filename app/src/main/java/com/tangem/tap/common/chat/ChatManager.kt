package com.tangem.tap.common.chat

import android.content.Context
import android.os.Build
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.config.models.ChatConfig
import com.tangem.datasource.config.models.SprinklrConfig
import com.tangem.datasource.config.models.ZendeskConfig
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.tap.common.chat.opener.implementation.SprinklrChatOpener
import com.tangem.tap.common.chat.opener.implementation.ZendeskChatOpener
import java.io.File

class ChatManager(
    private val preferencesStorage: PreferencesDataSource,
    private val foregroundActivityObserver: ForegroundActivityObserver,
) {
    private val openers = mutableMapOf<ChatConfig, ChatOpener>()

    fun open(config: ChatConfig, createLogsFile: (Context) -> File?, createFeedbackFile: (Context) -> File?) {
        val opener = openers.getOrPut(config) {
            when (config) {
                is SprinklrConfig -> SprinklrChatOpener(config, foregroundActivityObserver)
                is ZendeskConfig -> ZendeskChatOpener(getZendeskUserId(), config, foregroundActivityObserver)
            }
        }

        opener.open(createFeedbackFile, createLogsFile)
    }

    private fun getZendeskUserId(): String {
        if (preferencesStorage.zendeskFirstLaunchTime == null) {
            preferencesStorage.zendeskFirstLaunchTime = System.currentTimeMillis()
        }

        return "${preferencesStorage.zendeskFirstLaunchTime}${Build.MODEL}".hashCode().toString()
    }
}