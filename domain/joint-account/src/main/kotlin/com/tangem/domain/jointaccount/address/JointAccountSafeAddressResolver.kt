package com.tangem.domain.jointaccount.address

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Resolves the Safe address behind a joint account's network — the network at the owner key path of the account.
 * The address is what the balance is read by; a pending account has none yet and is absent from the result.
 */
interface JointAccountSafeAddressResolver {

    /** The Safe address of each joint network in [networks] that has one; other networks are ignored */
    suspend fun resolve(userWalletId: UserWalletId, networks: Set<Network>): Map<Network, String>
}