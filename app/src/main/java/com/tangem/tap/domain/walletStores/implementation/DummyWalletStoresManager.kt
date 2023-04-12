package com.tangem.tap.domain.walletStores.implementation

import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.features.wallet.models.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class DummyWalletStoresManager : WalletStoresManager {
    override fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>> {
        return emptyFlow()
    }

    override fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>> {
        return emptyFlow()
    }

    override suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel> {
        return emptyList()
    }

    override suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun clear(): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun fetch(userWallet: UserWallet, refresh: Boolean): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun fetch(userWallets: List<UserWallet>, refresh: Boolean): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun updateAmounts(userWallets: List<UserWallet>): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun updateSelectedAddress(
        userWalletId: UserWalletId,
        currency: Currency,
        addressType: AddressType,
    ): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }
}
