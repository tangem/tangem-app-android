package com.tangem.data.collectibles.account.derivation

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.isCollectibles
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerKeysRepository
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import javax.inject.Inject

internal class DefaultCollectiblesOwnerKeysRepository @Inject constructor(
    private val collectiblesAccountRepository: CollectiblesAccountRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles,
) : CollectiblesOwnerKeysRepository {

    override suspend fun missingOwnerKeysCount(
        collectiblesAccounts: List<Account.Collectibles>,
        userWallet: UserWallet,
    ): Int {
        if (collectiblesAccounts.isEmpty()) return 0

        return if (findMissingSeedKey(userWallet) != null) 1 else 0
    }

    override suspend fun findMissingOwnerKeyDerivation(
        userWalletId: UserWalletId,
    ): Map<ByteArrayKey, List<DerivationPath>> {
        if (!collectiblesFeatureToggles.isCollectiblesEnabled) return emptyMap()

        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return emptyMap()
        if (!isAccountKnown(userWalletId)) return emptyMap()

        val seedKey = findMissingSeedKey(userWallet) ?: return emptyMap()

        return mapOf(seedKey to listOf(path))
    }

    private suspend fun findMissingSeedKey(userWallet: UserWallet): ByteArrayKey? {
        val seedKey = userWallet.solanaSeedKey()?.let(::ByteArrayKey) ?: return null
        val existingPaths = derivationsRepository.getExistingDerivedKeys(userWallet, seedKey).keys

        return seedKey.takeIf { path !in existingPaths }
    }

    /** A re-added wallet has no record here yet: the backend document is what says the account exists */
    private suspend fun isAccountKnown(userWalletId: UserWalletId): Boolean {
        return collectiblesAccountRepository.getStatusSync(userWalletId).isAccountCreated ||
            walletAccountsFetcher.getSaved(userWalletId)?.accounts.orEmpty().any { it.isCollectibles }
    }

    private companion object {

        val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)
    }
}