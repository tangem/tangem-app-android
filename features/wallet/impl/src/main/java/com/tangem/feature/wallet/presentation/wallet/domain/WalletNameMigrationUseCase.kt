package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.wallets.manager.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import timber.log.Timber

class WalletNameMigrationUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val walletNamesMigrationRepository: WalletNamesMigrationRepository,
) {

    suspend operator fun invoke() {
        val wallets = userWalletsListManager.userWalletsSync

        if (walletNamesMigrationRepository.isMigrationDone()) {
            return
        }

        val existingNames: MutableSet<String> = mutableSetOf()
        wallets.indices.forEach { i ->
            val defaultName = wallets[i].name
            val suggestedWalletName = suggestedWalletName(defaultName, existingNames)
            if (defaultName != suggestedWalletName) {
                userWalletsListManager.update(wallets[i].walletId) { it.copy(name = suggestedWalletName) }
            }
            Timber.tag("here").e(i.toString() + " " + suggestedWalletName)
        }

        walletNamesMigrationRepository.setMigrationDone()
    }

    private fun suggestedWalletName(defaultName: String, existingNames: MutableSet<String>): String {
        val startIndex = 1
        for (index in startIndex..10000) {
            val name = if (index == startIndex) defaultName else "$defaultName $index"

            if (!existingNames.contains(name)) {
                existingNames.add(name)
                return name
            }
        }

        return defaultName
    }
}
