package com.tangem.data.jointaccount.derivation

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.usecase.primarySecp256k1PublicKey
import com.tangem.domain.wallets.derivations.DerivationsRepository
import javax.inject.Inject

internal class DefaultJointOwnerKeysRepository @Inject constructor(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
) : JointOwnerKeysRepository {

    override suspend fun missingOwnerKeysCount(jointAccounts: List<Account.Joint>, userWallet: UserWallet): Int {
        return findMissingPaths(jointAccounts, userWallet)?.second?.size ?: 0
    }

    override suspend fun findMissingOwnerKeyDerivations(
        userWalletId: UserWalletId,
    ): Map<ByteArrayKey, List<DerivationPath>> {
        val jointAccounts = singleAccountListSupplier.getSyncOrNull(userWalletId)
            ?.accounts
            .orEmpty()
            .filterIsInstance<Account.Joint>()
        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return emptyMap()

        val (seedKey, missingPaths) = findMissingPaths(jointAccounts, userWallet) ?: return emptyMap()
        return mapOf(seedKey to missingPaths)
    }

    private suspend fun findMissingPaths(
        jointAccounts: List<Account.Joint>,
        userWallet: UserWallet,
    ): Pair<ByteArrayKey, List<DerivationPath>>? {
        val ownerKeyIndices = jointAccounts.map { it.ownerKeyIndex.value }.distinct()
        if (ownerKeyIndices.isEmpty()) return null

        val seedKey = userWallet.primarySecp256k1PublicKey()?.let(::ByteArrayKey) ?: return null

        val existingPaths = derivationsRepository.getExistingDerivedKeys(userWallet, seedKey).keys
        val missingPaths = ownerKeyIndices
            .map { index -> jointAccountOwnerDerivationPath(index = index) }
            .filterNot { it in existingPaths }

        return if (missingPaths.isEmpty()) null else seedKey to missingPaths
    }
}