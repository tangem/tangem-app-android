package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.filterNotNull

/**
 * Reports every saved wallet the app shows the backup-problem banner for — typically left so by a build that
 * predates card-backup reporting — so another device or another card of the wallet can see the problem.
 *
 * A locked wallet is skipped until it is unlocked, because its card is stored without wallets and the curves would
 * be lost. A card already reported in the same state is not reported again.
 */
class ReportSavedWalletsBackupUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val reportSeenWalletCardUseCase: ReportSeenWalletCardUseCase,
) {

    /** Watches the saved wallets for as long as the caller's scope lives */
    suspend operator fun invoke() {
        userWalletsListRepository.userWallets
            .filterNotNull()
            .collect { userWallets ->
                userWallets
                    .filterIsInstance<UserWallet.Cold>()
                    .filterNot { it.isLocked }
                    .forEach { reportSeenWalletCardUseCase(userWallet = it, isNewWallet = false) }
            }
    }
}