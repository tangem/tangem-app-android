package com.tangem.data.wallets.derivations

import com.tangem.common.CompletionResult
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.map
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.derivations.ColdMapDerivationsRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.derivations.HotMapDerivationsRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultDerivationsRepository @Inject constructor(
    private val userWalletsStore: UserWalletsStore,
    private val hotDerivationsRepository: HotMapDerivationsRepository,
    private val coldDerivationsRepository: ColdMapDerivationsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : DerivationsRepository {

    override suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        derivePublicKeysByNetworks(userWalletId = userWalletId, networks = currencies.map(CryptoCurrency::network))
    }

    override suspend fun derivePublicKeysByNetworkIds(userWalletId: UserWalletId, networkIds: List<Network.RawID>) {
        val userWallet = userWalletsStore.getSyncStrict(userWalletId)
        when (userWallet) {
            is UserWallet.Cold -> coldDerivationsRepository.derivePublicKeysByNetworkIds(userWallet, networkIds)
            is UserWallet.Hot -> hotDerivationsRepository.derivePublicKeysByNetworkIds(userWallet, networkIds)
        }.also {
            userWallet.update(it)
        }
    }

    override suspend fun derivePublicKeysByNetworks(userWalletId: UserWalletId, networks: List<Network>) {
        val userWallet = userWalletsStore.getSyncStrict(userWalletId)
        when (userWallet) {
            is UserWallet.Cold -> coldDerivationsRepository.derivePublicKeysByNetworks(userWallet, networks)
            is UserWallet.Hot -> hotDerivationsRepository.derivePublicKeysByNetworks(userWallet, networks)
        }.also {
            userWallet.update(it)
        }
    }

    override suspend fun derivePublicKeys(
        userWalletId: UserWalletId,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap> {
        val userWallet = userWalletsStore.getSyncStrict(userWalletId)
        return when (userWallet) {
            is UserWallet.Cold -> coldDerivationsRepository.derivePublicKeys(userWallet, derivations)
            is UserWallet.Hot -> hotDerivationsRepository.derivePublicKeys(userWallet, derivations)
        }.let {
            userWallet.update(it.first)
            it.second
        }
    }

    override suspend fun hasMissedDerivations(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean {
        return when (val userWallet = userWalletsStore.getSyncStrict(userWalletId)) {
            is UserWallet.Cold -> coldDerivationsRepository.hasMissedDerivations(userWallet, networksWithDerivationPath)
            is UserWallet.Hot -> hotDerivationsRepository.hasMissedDerivations(userWallet, networksWithDerivationPath)
        }
    }

    private suspend fun UserWallet.update(newUserWallet: UserWallet) = withContext(dispatchers.io) {
        check(this@update.walletId == newUserWallet.walletId) {
            "Cannot update UserWallet with different walletId: ${newUserWallet.walletId}"
        }

        if (this@update == newUserWallet) {
            return@withContext // No update needed
        }

        val updateResult = userWalletsStore.update(
            userWalletId = newUserWallet.walletId,
            update = { userWalletToUpdate -> newUserWallet },
        )

        when (updateResult) {
            is CompletionResult.Failure -> throw updateResult.error
            is CompletionResult.Success -> updateResult.data
        }
    }
}