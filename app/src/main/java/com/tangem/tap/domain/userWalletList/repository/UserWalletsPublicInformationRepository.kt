package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation

internal interface UserWalletsPublicInformationRepository {
    suspend fun save(userWallet: UserWallet): CompletionResult<Unit>

    suspend fun getAll(): CompletionResult<List<UserWalletPublicInformation>>

    suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit>
    suspend fun clear(): CompletionResult<Unit>
}