package com.tangem.tap.common.chat

import android.content.Context
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.tap.common.chat.opener.implementation.SprinklrChatOpener
import com.tangem.tap.common.chat.opener.implementation.ZendeskChatOpener
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.persistence.PreferencesStorage
import org.rekotlin.Store

class ChatManager(
    private val preferencesStorage: PreferencesStorage,
    private val foregroundActivityObserver: ForegroundActivityObserver,
    private val store: Store<AppState>,
) {
    private val openers = mutableMapOf<ChatConfig, ChatOpener>()

    fun open(config: ChatConfig, feedbackDataBuilder: (Context) -> String) {
        val opener = openers.getOrPut(config) {
            when (config) {
                is SprinklrConfig -> SprinklrChatOpener(config, store)
                is ZendeskConfig -> ZendeskChatOpener(config, preferencesStorage, foregroundActivityObserver)
            }
        }

        opener.open(feedbackDataBuilder)
    }
}