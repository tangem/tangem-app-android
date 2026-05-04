package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.copy
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.utils.logging.TangemLogger

class WalletNameMigrationUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletNamesMigrationRepository: WalletNamesMigrationRepository,
) {

    suspend operator fun invoke() {
        if (walletNamesMigrationRepository.isMigrationDone()) {
            return
        }

        val wallets = userWalletsListRepository.userWalletsSync()
        val existingNames: MutableSet<String> = mutableSetOf()
        wallets.forEach { wallet ->
            val defaultName = wallet.name
            val suggestedWalletName = suggestedWalletName(defaultName, existingNames)
            if (defaultName != suggestedWalletName) {
                userWalletsListRepository.saveWithoutLock(wallet.copy(name = suggestedWalletName), canOverride = true)
            }
            TangemLogger.withTag("Migrated names").e(wallet.walletId.toString() + " " + suggestedWalletName)
        }

        walletNamesMigrationRepository.setMigrationDone()
    }

    private fun suggestedWalletName(defaultName: String, existingNames: MutableSet<String>): String {
        val startIndex = 1
        for (index in startIndex..MAX_WALLETS_LIMIT) {
            val name = if (index == startIndex) defaultName else "$defaultName $index"

            if (!existingNames.contains(name)) {
                existingNames.add(name)
                return name
            }
        }

        return defaultName
    }

    companion object {
        const val MAX_WALLETS_LIMIT = 10000
    }
}