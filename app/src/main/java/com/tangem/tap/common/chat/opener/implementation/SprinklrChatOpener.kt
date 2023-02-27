package com.tangem.tap.common.chat.opener.implementation

import android.content.Context
import android.content.Intent
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.datasource.config.models.SprinklrConfig
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.sprinklr.redux.SprinklrAction
import com.tangem.tap.features.sprinklr.ui.SprinklrActivity
import com.tangem.tap.withForegroundActivity
import org.rekotlin.Store

internal class SprinklrChatOpener(
    private val userId: String,
    private val config: SprinklrConfig,
    private val store: Store<AppState>,
    private val foregroundActivityObserver: ForegroundActivityObserver,
) : ChatOpener {
    override fun open(feedbackDataBuilder: (Context) -> String) {
        store.dispatch(SprinklrAction.Init(userId, config))
        foregroundActivityObserver.withForegroundActivity { activity ->
            val intent = Intent(activity, SprinklrActivity::class.java)
            activity.startActivity(intent)
        }
    }
}