package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Input of the Discovery feed.
 *
 * @property userWalletId wallet the feature was opened for; constant for the feature's lifetime. Carried for
 *  the screens that will need it — balances, "my predictions", signing. Nothing reads it yet.
 */
internal data class PolymarketMainParams(
    val userWalletId: UserWalletId,
)