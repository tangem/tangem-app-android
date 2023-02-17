package com.tangem.tap.features.sprinklr.redux

import com.tangem.tap.common.chat.SprinklrConfig
import org.rekotlin.Action

sealed interface SprinklrAction : Action {
    data class SetConfig(val config: SprinklrConfig) : SprinklrAction
    data class UpdateUrl(val url: String) : SprinklrAction
}