package com.tangem.domain.transaction

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.ParsedQrCode
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Wallet address service repository.
 */
interface WalletAddressServiceRepository {

    suspend fun validateAddress(userWalletId: UserWalletId, network: Network, address: String): Boolean

    fun validateMemo(network: Network, memo: String): Boolean

    suspend fun parseSharedAddress(input: String, network: Network): ParsedQrCode
}