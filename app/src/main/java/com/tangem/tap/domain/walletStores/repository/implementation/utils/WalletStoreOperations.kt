package com.tangem.tap.domain.walletStores.repository.implementation.utils

import com.tangem.blockchain.common.Wallet
import com.tangem.common.core.TangemError
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import timber.log.Timber

internal inline fun HashMap<UserWalletId, List<WalletStoreModel>>.replaceWalletStore(
    walletStoreToUpdate: WalletStoreModel,
    update: (walletStore: WalletStoreModel) -> WalletStoreModel,
): HashMap<UserWalletId, List<WalletStoreModel>> {
    return replaceWalletStores(listOf(walletStoreToUpdate), update)
}

internal inline fun HashMap<UserWalletId, List<WalletStoreModel>>.replaceWalletStores(
    walletStoresToUpdate: List<WalletStoreModel>,
    update: (walletStore: WalletStoreModel) -> WalletStoreModel,
): HashMap<UserWalletId, List<WalletStoreModel>> {
    return this.apply {
        val currentWalletStores = this
        walletStoresToUpdate
            .groupBy { it.userWalletId }
            .forEach { (userWalletId, walletStoresToUpdate) ->
                currentWalletStores[userWalletId] = currentWalletStores[userWalletId]
                    ?.replaceWalletStores(walletStoresToUpdate, update)
                    .orEmpty()
            }
    }
}

internal fun WalletStoreModel.updateWithError(
    wallet: Wallet,
    error: TangemError,
): WalletStoreModel {
    return this.copy(
        walletsData = walletsData.updateWithError(
            wallet = wallet,
            error = error,
        ),
    )
}

internal fun WalletStoreModel.updateWithAmounts(
    wallet: Wallet,
): WalletStoreModel {
    return this.copy(
        walletsData = walletsData.updateWithAmounts(wallet = wallet),
    )
}

internal fun WalletStoreModel.updateWithFiatRates(
    rates: Map<String, Double>,
): WalletStoreModel {
    return this.copy(
        walletsData = walletsData.updateWithFiatRates(rates),
    )
}

internal fun WalletStoreModel.updateWithSelf(
    newWalletStore: WalletStoreModel,
): WalletStoreModel {
    val oldStore = this
    return oldStore.copy(
        derivationPath = newWalletStore.derivationPath,
        walletsData = oldStore.walletsData.updateWithSelf(newWalletStore.walletsData),
        walletRent = newWalletStore.walletRent,
        blockchainNetwork = newWalletStore.blockchainNetwork,
        walletManager = newWalletStore.walletManager,
    )
}

internal fun WalletStoreModel.updateWithMissedDerivation(): WalletStoreModel {
    return this.copy(
        walletsData = walletsData.updateWithMissedDerivation(),
    )
}

internal fun WalletStoreModel.updateWithUnreachable(): WalletStoreModel {
    return this.copy(
        walletsData = walletsData.updateWithUnreachable(),
    )
}

internal fun WalletStoreModel.updateWithRent(rent: WalletStoreModel.WalletRent?): WalletStoreModel {
    return this.copy(
        walletRent = rent,
    )
}

internal fun WalletStoreModel.isSameWalletStore(other: WalletStoreModel): Boolean {
    return this.blockchain == other.blockchain &&
        this.derivationPath == other.derivationPath
}

private inline fun List<WalletStoreModel>.replaceWalletStores(
    walletStoresToUpdate: List<WalletStoreModel>,
    update: (walletStore: WalletStoreModel) -> WalletStoreModel,
): List<WalletStoreModel> {
    val mutableStores = ArrayList<WalletStoreModel>(this)

    walletStoresToUpdate
        .asSequence()
        .map { it.blockchain to it.derivationPath }
        .forEach { (blockchain, derivationPath) ->
            val index = mutableStores.indexOfFirst {
                it.blockchain == blockchain && it.derivationPath == derivationPath
            }
            val currentWalletStore = mutableStores[index]
            val updatedWalletStore = update(currentWalletStore)

            if (currentWalletStore != updatedWalletStore) {
                Timber.d(
                    """
                        Update wallet store in storage
                        |- User wallet ID: ${updatedWalletStore.userWalletId}
                        |- Blockchain: ${updatedWalletStore.blockchain}
                    """.trimIndent(),
                )

                mutableStores[index] = updatedWalletStore
            }
        }

    return mutableStores
}
