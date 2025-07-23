package com.tangem.domain.transaction

import com.tangem.blockchain.common.ResolveAddressResult
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.ParsedQrCode
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Wallet address service repository.
 */
interface WalletAddressServiceRepository {

    suspend fun resolveAddress(userWalletId: UserWalletId, network: Network, address: String): ResolveAddressResult

    suspend fun validateAddress(userWalletId: UserWalletId, network: Network, address: String): Boolean

    fun validateMemo(network: Network, memo: String): Boolean

    suspend fun parseSharedAddress(input: String, network: Network): ParsedQrCode
}