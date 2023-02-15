package com.tangem.tap.common.chat.opener

import android.content.Context

internal interface ChatOpener {
    fun open(feedbackDataBuilder: (Context) -> String)
}