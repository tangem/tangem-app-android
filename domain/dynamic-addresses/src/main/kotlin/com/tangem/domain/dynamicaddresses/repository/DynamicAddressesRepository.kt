package com.tangem.domain.dynamicaddresses.repository

import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface DynamicAddressesRepository {

    fun getStatus(userWalletId: UserWalletId, network: Network): Flow<DynamicAddressesStatus>

    suspend fun enable(userWalletId: UserWalletId, network: Network, xpub: String)

    suspend fun disable(userWalletId: UserWalletId, network: Network)

    suspend fun getReceiveAddress(userWalletId: UserWalletId, network: Network): String

    // for explorer url
    suspend fun getLastUsedReceiveAddress(userWalletId: UserWalletId, network: Network): String?

    /**
     * Every XPUB-derived address already used on [network], empty when dynamic addresses are off there or the network
     * does not support them. These addresses live in the wallet manager only and never reach `NetworkAddress`, so a
     * caller that has to recognize the wallet's own addresses must ask for them here.
     */
    suspend fun getUsedAddresses(userWalletId: UserWalletId, network: Network): List<String>

    suspend fun hasNonBaseBalances(userWalletId: UserWalletId, network: Network): Boolean

    /** Returns true if there are custom tokens with change/index ≠ 0 that conflict with dynamic addresses */
    suspend fun hasConflictingCustomTokens(userWalletId: UserWalletId, network: Network): Boolean

    /** Lightweight check: is the DA flag enabled for the native coin of the given network (no xpub availability check) */
    fun isDynamicAddressesEnabledForNetwork(userWalletId: UserWalletId, networkId: Network.ID): Flow<Boolean>

    /**
     * Emits true when dynamic addresses are DISABLED for this token but a silent xpub probe
     * detected non-zero balances on derived addresses beyond the base one. Only positive
     * results are cached per session; false results and probe failures are not cached and may
     * be re-probed on subsequent collections or when status changes.
     */
    fun hasFundsOnAdditionalAddresses(userWalletId: UserWalletId, network: Network): Flow<Boolean>
}