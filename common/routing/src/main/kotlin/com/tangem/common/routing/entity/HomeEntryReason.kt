package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

/** Why the app lands on the screen for a user without wallets. */
@Serializable
enum class HomeEntryReason {
    Launch,
    LastWalletForgotten,
}