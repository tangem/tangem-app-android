package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.wallet.UserWalletId

/** Previews an invite before the invitee decides to join. */
class GetJointAccountInvitePreviewUseCase(private val repository: JointAccountRepository) {

    suspend operator fun invoke(
        walletId: UserWalletId,
        inviteId: String,
    ): Either<JointAccountInvitePreviewError, JointAccountInvitePreview> =
        repository.getInvitePreview(walletId = walletId, inviteId = inviteId)
}