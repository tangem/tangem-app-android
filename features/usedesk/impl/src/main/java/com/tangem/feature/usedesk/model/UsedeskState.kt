package com.tangem.feature.usedesk.model

import androidx.compose.runtime.Stable
import ru.usedesk.chat_sdk.entity.UsedeskChatConfiguration

@Stable
internal data class UsedeskState(
    val usedeskChatConfiguration: UsedeskChatConfiguration,
)