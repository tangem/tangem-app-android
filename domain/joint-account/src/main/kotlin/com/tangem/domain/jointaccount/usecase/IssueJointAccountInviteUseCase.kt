package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccountInviteIssueError
import com.tangem.domain.jointaccount.model.JointAccountIssuedInvite
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger

/** Refetches the list after issuing, and after a refusal too: either way the slot state on this device is stale */
class IssueJointAccountInviteUseCase(
    private val repository: JointAccountRepository,
    private val fetcher: SingleJointAccountListFetcher,
) {

    suspend operator fun invoke(
        accountId: AccountId,
        slot: Int,
    ): Either<JointAccountInviteIssueError, JointAccountIssuedInvite> {
        val result = repository.issueInvite(
            userWalletId = accountId.userWalletId,
            cryptoAccountId = accountId.value,
            slot = slot,
        )
        val isSlotStateStale = result.isRight() || result.leftOrNull() is JointAccountInviteIssueError.SlotUnavailable

        if (isSlotStateStale) refetch(userWalletId = accountId.userWalletId)

        return result
    }

    private suspend fun refetch(userWalletId: UserWalletId) {
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).onLeft {
            TangemLogger.e("Failed to refetch the joint accounts of $userWalletId after issuing an invite", it)
        }
    }
}