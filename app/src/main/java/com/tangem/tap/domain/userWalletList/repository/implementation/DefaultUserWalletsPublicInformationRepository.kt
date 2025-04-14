package com.tangem.tap.domain.userWalletList.repository.implementation

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.flatMap
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation
import com.tangem.tap.domain.userWalletList.repository.UserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.utils.publicInformation
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DefaultUserWalletsPublicInformationRepository(
    moshi: Moshi,
    private val secureStorage: SecureStorage,
) : UserWalletsPublicInformationRepository {

    private val publicInformationAdapter: JsonAdapter<List<UserWalletPublicInformation>> by lazy {
        moshi.adapter(
            Types.newParameterizedType(List::class.java, UserWalletPublicInformation::class.java),
        )
    }

    override suspend fun save(userWallet: UserWallet, canOverride: Boolean): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            getAll().flatMap { savedWalletsInformation ->
                val infoToSave = if (canOverride) {
                    savedWalletsInformation.addOrReplace(userWallet.publicInformation) {
                        it.walletId == userWallet.walletId
                    }
                } else {
                    if (savedWalletsInformation.none { it.walletId == userWallet.walletId }) {
                        savedWalletsInformation + userWallet.publicInformation
                    } else {
                        return@withContext CompletionResult.Success(Unit)
                    }
                }

                save(infoToSave)
            }
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
        return withContext(Dispatchers.IO) {
            getAll()
                .flatMap { publicInformation ->
                    val infoToRemove = publicInformation
                        .filter { it.walletId in walletIds }
                        .toSet()

                    save(
                        publicInformation = publicInformation - infoToRemove,
                    )
                }
        }
    }

    override suspend fun clear(): CompletionResult<Unit> = catching {
        withContext(Dispatchers.IO) {
            secureStorage.delete(StorageKey.UserWalletPublicInformation.name)
        }
    }

    @JvmName("saveWithPublicInformation")
    private suspend fun save(publicInformation: List<UserWalletPublicInformation>): CompletionResult<Unit> = catching {
        withContext(Dispatchers.IO) {
            publicInformation
                .let(publicInformationAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
                .also { secureStorage.store(it, StorageKey.UserWalletPublicInformation.name) }
        }
    }

    private enum class StorageKey {
        UserWalletPublicInformation,
    }
}