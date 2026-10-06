package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

class GetSharedJointAccountInvitesUseCase(private val repository: JointAccountRepository) {

    operator fun invoke(userWalletId: UserWalletId, cryptoAccountId: String): Flow<Set<String>> {
        return repository.getSharedInvites(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId)
    }
}