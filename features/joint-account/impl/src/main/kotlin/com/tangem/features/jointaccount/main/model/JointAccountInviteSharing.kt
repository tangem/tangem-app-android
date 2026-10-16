package com.tangem.features.jointaccount.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.MarkJointAccountInviteSharedUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.deeplink.JointAccountInviteLinkBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

/** Shares a slot's invite message and records the invite once the user picks where to send it */
@ModelScoped
internal class JointAccountInviteSharing @Inject constructor(
    private val shareManager: ShareManager,
    private val getJointAccountInvitesUseCase: GetJointAccountInvitesUseCase,
    private val markJointAccountInviteSharedUseCase: MarkJointAccountInviteSharedUseCase,
) {

    /** The slot's own invite: a link issued for one slot cannot be redeemed for another */
    fun message(inviteId: String, accountName: TextReference): TextReference = resourceReference(
        id = CoreUiR.string.joint_account_invite_member_message,
        formatArgs = wrappedList(accountName, JointAccountInviteLinkBuilder.build(inviteId = inviteId)),
    )

    fun share(message: String) {
        shareManager.shareTrackedText(text = message)
    }

    /**
     * Records the invites of [account] whose link was sent, not when the share sheet merely opens. Keyed to the
     * account id: the account updates on every poll, and restarting on each would cancel a write or drop a pick
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun recordShared(userWalletId: UserWalletId, account: Flow<JointAccount?>): Flow<Unit> = account
        .mapNotNull { it?.cryptoAccountId }
        .distinctUntilChanged()
        .flatMapLatest { cryptoAccountId ->
            shareManager.sharedTexts
                .mapNotNull { text ->
                    getJointAccountInvitesUseCase(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId)
                        .firstOrNull { inviteId -> text.containsLink(JointAccountInviteLinkBuilder.build(inviteId)) }
                }
                .map { inviteId ->
                    markJointAccountInviteSharedUseCase(
                        userWalletId = userWalletId,
                        cryptoAccountId = cryptoAccountId,
                        inviteId = inviteId,
                    )
                }
        }
}

/** A whole word of the message, so one invite's link never matches another's that merely starts with it */
private fun String.containsLink(link: String): Boolean = split(WHITESPACE).any { word -> word == link }

private val WHITESPACE = Regex(pattern = "\\s+")