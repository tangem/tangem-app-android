package com.tangem.data.collectibles.delegate

import com.tangem.domain.models.account.AccountId
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId

/**
 * Addresses a collectibles account's delegate keypair: the account it belongs to and its entry in the hot SDK
 * keystore.
 */
internal data class CollectiblesHotWalletId(val accountId: AccountId, private val keystoreId: String) {

    /** The delegate is always created and unlocked with [HotAuth.NoAuth], so its auth type is a constant. */
    val hotWalletId = HotWalletId(value = keystoreId, authType = HotWalletId.AuthType.NoPassword)
}