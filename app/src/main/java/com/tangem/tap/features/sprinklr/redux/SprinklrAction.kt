package com.tangem.tap.features.sprinklr.redux

import com.tangem.datasource.config.models.SprinklrConfig
import org.rekotlin.Action

sealed interface SprinklrAction : Action {
    data class Init(val userId: String, val config: SprinklrConfig) : SprinklrAction
    data class UpdateUrl(val url: String) : SprinklrAction
    data class UpdateSprinklrDomains(val domains: List<String>) : SprinklrAction
}
