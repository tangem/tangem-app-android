package com.tangem.domain.offramp.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * A locally-recorded marker that the app itself initiated a sell (off-ramp) flow.
 *

 * redirects back via the `redirect_sell` deeplink, the returned `request_id` is matched against a stored
 * [PendingOfframp] to prove the redirect corresponds to a real, user-initiated sell.
 *
 * @property requestId    self-issued single-use nonce embedded in the provider redirect URL
 * @property userWalletId wallet that initiated the sell
 * @property currencyId   [com.tangem.domain.models.currency.CryptoCurrency.ID.value] being sold

 */
data class PendingOfframp(
    val requestId: String,
    val userWalletId: UserWalletId,
    val currencyId: String,
    val createdAt: Long,
)