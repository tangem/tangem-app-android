package com.tangem.domain.transaction

import com.tangem.blockchain.common.ResolveAddressResult
import com.tangem.blockchain.common.ReverseResolveAddressResult
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.ParsedQrCode

/**
 * Wallet address service repository.
 */
interface WalletAddressServiceRepository {

    suspend fun getEns(userWalletId: UserWalletId, network: Network, address: String): String?

    suspend fun reverseResolveAddress(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
    ): ReverseResolveAddressResult

    suspend fun resolveAddress(userWalletId: UserWalletId, network: Network, address: String): ResolveAddressResult

    suspend fun validateAddress(userWalletId: UserWalletId, network: Network, address: String): Boolean

    fun validateMemo(network: Network, memo: String): Boolean

    suspend fun parseSharedAddress(input: String, network: Network): ParsedQrCode
}