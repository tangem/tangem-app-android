package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet

/** Number of distinct owner-key derivations among [jointAccounts] that are missing on [userWallet] */
class GetMissingJointOwnerKeysCountUseCase(
    private val repository: JointOwnerKeysRepository,
) {

    suspend operator fun invoke(jointAccounts: List<Account.Joint>, userWallet: UserWallet): Int =
        repository.missingOwnerKeysCount(jointAccounts, userWallet)
}