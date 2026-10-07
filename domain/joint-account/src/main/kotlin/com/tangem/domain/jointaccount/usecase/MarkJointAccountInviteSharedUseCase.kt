package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.wallet.UserWalletId

class MarkJointAccountInviteSharedUseCase(private val repository: JointAccountRepository) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoAccountId: String, inviteId: String) {
        repository.markInviteShared(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId, inviteId = inviteId)
    }
}