package com.tangem.data.collectibles.account.cleaner

import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Removes the recorded collectibles account and the hot delegate key of deleted wallets.
 *
 * [UserWalletId] is derived from the wallet key, so re-adding the same wallet reproduces the storage key: a
 * re-added wallet would open straight on the main screen and sign spins with a delegate the vault may no longer
 * point at. The owner key needs no clearing — it lives in the wallet's derivations, which go with the wallet.
 *
 * @property accountRepository record of whether the wallet has a collectibles account
 * @property hotDelegate       custodian of the vault's `hot_delegate` key
 */
internal class CollectiblesUserWalletDataCleaner @Inject constructor(
    private val accountRepository: CollectiblesAccountRepository,
    private val hotDelegate: CollectiblesHotDelegate,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        userWalletIds.forEach { userWalletId ->
            // The delegate goes first: its record is part of the account's entry, and clearing that entry would
            // leave the keystore key with nothing pointing at it.
            val accountId = AccountId.forCollectiblesAccount(userWalletId = userWalletId)

            runSuspendCatching { hotDelegate.forget(accountId) }
                .onFailure { TangemLogger.e("Failed to clear the collectibles delegate of $userWalletId", it) }

            runSuspendCatching { accountRepository.clearAccount(userWalletId) }
                .onFailure { TangemLogger.e("Failed to clear the collectibles account of $userWalletId", it) }
        }
    }
}