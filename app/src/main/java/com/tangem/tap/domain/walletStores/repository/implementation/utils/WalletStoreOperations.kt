package com.tangem.tap.domain.walletStores.repository.implementation.utils

import com.tangem.blockchain.common.Wallet
import com.tangem.common.core.TangemError
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel

internal inline fun HashMap<UserWalletId, List<WalletStoreModel>>.replaceWalletStore(
    walletId: UserWalletId,
    walletStore: WalletStoreModel,
    update: (walletStore: WalletStoreModel) -> WalletStoreModel,
): HashMap<UserWalletId, List<WalletStoreModel>> {
    return this.apply {
        this[walletId] = this[walletId]
            ?.replaceWalletStore(walletStore, update)
            .orEmpty()
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
        walletManager = newWalletStore.walletManager,
        walletRent = newWalletStore.walletRent,
        walletsData = oldStore.walletsData.updateWithSelf(newWalletStore.walletsData),
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

internal inline fun List<WalletStoreModel>.replaceWalletStore(
    newWalletStore: WalletStoreModel,
    update: (walletStore: WalletStoreModel) -> WalletStoreModel,
): List<WalletStoreModel> {
    val mutableStores = ArrayList(this)
    for ((index, walletStore) in this.withIndex()) {
        if (walletStore.isSameWalletStore(newWalletStore)) {
            mutableStores[index] = update(walletStore)
            break
        }
    }
    return mutableStores
}

internal fun WalletStoreModel.isSameWalletStore(other: WalletStoreModel): Boolean {
    return blockchainNetwork == other.blockchainNetwork
}
