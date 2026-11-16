package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerKeysRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import javax.inject.Inject

/** Number of [collectiblesAccounts] whose owner key is missing on [userWallet]; a wallet has one at most */
class GetMissingCollectiblesOwnerKeysCountUseCase @Inject constructor(
    private val repository: CollectiblesOwnerKeysRepository,
) {

    suspend operator fun invoke(collectiblesAccounts: List<Account.Collectibles>, userWallet: UserWallet): Int =
        repository.missingOwnerKeysCount(collectiblesAccounts, userWallet)
}