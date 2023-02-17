package com.tangem.tap.common.chat.opener.implementation

import android.content.Context
import com.tangem.tap.common.chat.SprinklrConfig
import com.tangem.tap.common.chat.opener.ChatOpener
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.sprinklr.redux.SprinklrAction
import org.rekotlin.Store

internal class SprinklrChatOpener(
    private val userId: String,
    private val config: SprinklrConfig,
    private val store: Store<AppState>,
) : ChatOpener {
    override fun open(feedbackDataBuilder: (Context) -> String) {
        store.dispatch(SprinklrAction.Init(userId, config))
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Sprinklr))
    }
}