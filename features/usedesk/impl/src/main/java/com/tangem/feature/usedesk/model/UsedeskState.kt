package com.tangem.feature.usedesk.model

import androidx.compose.runtime.Stable
import com.tangem.usedesk.chat_sdk.entity.UsedeskChatConfiguration

@Stable
internal data class UsedeskState(
    val usedeskChatConfiguration: UsedeskChatConfiguration? = null,
)