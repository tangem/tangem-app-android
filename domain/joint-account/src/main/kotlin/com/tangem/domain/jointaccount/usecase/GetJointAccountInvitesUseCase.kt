package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.wallet.UserWalletId

class GetJointAccountInvitesUseCase(private val repository: JointAccountRepository) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoAccountId: String): List<JointAccountInvite> {
        return repository.getInvites(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId)
    }
}