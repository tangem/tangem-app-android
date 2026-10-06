package com.tangem.tap.di.domain

import com.tangem.domain.feedback.models.SupportChatTopic
import dagger.MapKey

@MapKey
internal annotation class SupportChatTopicKey(val value: SupportChatTopic)