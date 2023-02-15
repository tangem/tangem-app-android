package com.tangem.tap.common.chat.opener.implementation

import android.content.Context
import com.tangem.tap.common.chat.opener.ChatOpener

internal class SprinklrChatOpener : ChatOpener {
    override fun open(feedbackDataBuilder: (Context) -> String) {
        // TODO: Open activity with Sprinklr WebView, will be in further MRs
    }
}