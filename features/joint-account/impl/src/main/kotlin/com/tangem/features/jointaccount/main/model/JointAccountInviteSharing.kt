package com.tangem.features.jointaccount.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.share.ShareManager
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.MarkJointAccountInviteSharedUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.deeplink.JointAccountInviteLinkBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

/** Shares a slot's invite link and records the invite once the user picks where to send it */
@ModelScoped
internal class JointAccountInviteSharing @Inject constructor(
    private val shareManager: ShareManager,
    private val getJointAccountInvitesUseCase: GetJointAccountInvitesUseCase,
    private val markJointAccountInviteSharedUseCase: MarkJointAccountInviteSharedUseCase,
) {

    /** The slot's own invite: a link issued for one slot cannot be redeemed for another */
    fun share(inviteId: String) {
        shareManager.shareTrackedText(text = JointAccountInviteLinkBuilder.build(inviteId = inviteId))
    }

    /** Records the invites of [account] whose link was sent, not when the share sheet merely opens */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun recordShared(userWalletId: UserWalletId, account: Flow<JointAccount?>): Flow<Unit> = account
        .filterNotNull()
        .flatMapLatest { jointAccount ->
            val invites = getJointAccountInvitesUseCase(
                userWalletId = userWalletId,
                cryptoAccountId = jointAccount.cryptoAccountId,
            )
            shareManager.sharedTexts
                .mapNotNull { text ->
                    invites.firstOrNull { inviteId -> JointAccountInviteLinkBuilder.build(inviteId = inviteId) == text }
                }
                .map { inviteId ->
                    markJointAccountInviteSharedUseCase(
                        userWalletId = userWalletId,
                        cryptoAccountId = jointAccount.cryptoAccountId,
                        inviteId = inviteId,
                    )
                }
        }
}