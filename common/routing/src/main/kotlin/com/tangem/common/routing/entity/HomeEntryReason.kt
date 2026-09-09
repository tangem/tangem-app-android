package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

@Serializable
enum class HomeEntryReason {
    Launch,
    LastWalletForgotten,
}