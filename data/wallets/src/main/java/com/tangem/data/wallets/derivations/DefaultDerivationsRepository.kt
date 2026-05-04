package com.tangem.data.wallets.derivations

import arrow.core.getOrElse
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.ColdMapDerivationsRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.derivations.HotMapDerivationsRepository
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultDerivationsRepository @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotDerivationsRepository: HotMapDerivationsRepository,
    private val coldDerivationsRepository: ColdMapDerivationsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : DerivationsRepository {

    override suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) {
            TangemLogger.d("Nothing to derive")
            return
        }

        derivePublicKeysByNetworks(userWalletId = userWalletId, networks = currencies.map(CryptoCurrency::network))
    }

    override suspend fun derivePublicKeysByNetworkIds(
        userWalletId: UserWalletId,
        networkIds: List<Network.RawID>,
        accountIndex: DerivationIndex,
    ) {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        when (userWallet) {
            is UserWallet.Cold -> coldDerivationsRepository.derivePublicKeysByNetworkIds(userWallet, networkIds)
            is UserWallet.Hot -> {
                hotDerivationsRepository.derivePublicKeysByNetworkIds(userWallet, networkIds, accountIndex)
            }
        }.also {
            userWallet.update(it)
        }
    }

    override suspend fun derivePublicKeysByNetworks(userWalletId: UserWalletId, networks: List<Network>) {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
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
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        return when (userWallet) {
            is UserWallet.Cold -> coldDerivationsRepository.derivePublicKeys(userWallet, derivations)
            is UserWallet.Hot -> hotDerivationsRepository.derivePublicKeys(userWallet, derivations)
        }.let { publicKeysMapByUserWallet ->
            userWallet.update(publicKeysMapByUserWallet.first)
            publicKeysMapByUserWallet.second
        }
    }

    override suspend fun hasMissedDerivations(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean {
        return when (val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)) {
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

        userWalletsListRepository.saveWithoutLock(userWallet = newUserWallet, canOverride = true)
            .getOrElse { throw IllegalStateException("Unable to update user wallet: $it") }
    }
}