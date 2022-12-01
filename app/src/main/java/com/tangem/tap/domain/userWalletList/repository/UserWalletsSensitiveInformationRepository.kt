package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation

internal interface UserWalletsSensitiveInformationRepository {
    suspend fun save(userWallet: UserWallet): CompletionResult<Unit>
    suspend fun getAll(
        encryptionKeys: List<UserWalletEncryptionKey>,
    ): CompletionResult<Map<UserWalletId, UserWalletSensitiveInformation>>

    suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit>
}
