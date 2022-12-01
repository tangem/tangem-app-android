package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.flatMap
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.extensions.replaceByOrAdd
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation
import com.tangem.tap.domain.userWalletList.repository.UserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.utils.publicInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DefaultUserWalletsPublicInformationRepository(
    moshi: Moshi,
    private val secureStorage: SecureStorage,
) : UserWalletsPublicInformationRepository {
    private val publicInformationAdapter: JsonAdapter<List<UserWalletPublicInformation>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, UserWalletPublicInformation::class.java),
    )

    override suspend fun save(userWallet: UserWallet): CompletionResult<Unit> {
        return getAll()
            .flatMap { savedInformation ->
                val infoToSave = withContext(Dispatchers.Default) {
                    savedInformation.toMutableList()
                        .apply {
                            replaceByOrAdd(userWallet.publicInformation) {
                                userWallet.walletId == it.walletId
                            }
                        }
                }

                save(infoToSave)
            }
    }

    override suspend fun getAll(): CompletionResult<List<UserWalletPublicInformation>> = catching {
        withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.UserWalletPublicInformation.name)
                ?.decodeToString()
                ?.let(publicInformationAdapter::fromJson)
                .orEmpty()
        }
    }

    override suspend fun delete(walletIds: List<UserWalletId>): CompletionResult<Unit> {
        return getAll()
            .flatMap { publicInformation ->
                val infoToRemove = publicInformation
                    .filter { it.walletId in walletIds }
                    .toSet()

                save(
                    publicInformation = publicInformation - infoToRemove,
                )
            }
    }

    override suspend fun clear(): CompletionResult<Unit> = catching {
        secureStorage.delete(StorageKey.UserWalletPublicInformation.name)
    }

    @JvmName("saveWithPublicInformation")
    private suspend fun save(
        publicInformation: List<UserWalletPublicInformation>,
    ): CompletionResult<Unit> = catching {
        withContext(Dispatchers.IO) {
            publicInformation
                .let(publicInformationAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
                .also { secureStorage.store(it, StorageKey.UserWalletPublicInformation.name) }
        }
    }

    private enum class StorageKey {
        UserWalletPublicInformation
    }
}
