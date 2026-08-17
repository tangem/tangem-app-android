package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

/**
 * Explicit mode for account top-up / withdraw flows opened on the swap screen.
 * TopUp anchors the account as an abstract TO; Withdraw anchors it as the concrete FROM.
 * Single Payment account per wallet — resolved by userWalletId on the swap side, no id needed here.
 */
@Serializable
sealed interface AccountFlow {
    @Serializable data object TopUp : AccountFlow

    @Serializable data object Withdraw : AccountFlow
}