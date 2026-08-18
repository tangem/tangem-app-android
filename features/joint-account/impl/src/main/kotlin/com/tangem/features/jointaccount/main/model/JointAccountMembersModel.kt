package com.tangem.features.jointaccount.main.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// TODO([REDACTED_TASK_KEY]): visible texts are hardcoded English literals while the screen runs on stubbed
//  state. Replace with resource references once the invite-members strings land in Lokalise.
@Stable
@ModelScoped
internal class JointAccountMembersModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<JointAccountMembersComponent.Params>()

    val uiState: StateFlow<JointAccountMembersUM>
        field = MutableStateFlow(createStubState())

    private fun createStubState(): JointAccountMembersUM {
        val isInviteMode = params.mode == JointAccountMembersComponent.Mode.Invite
        val canInvite = isInviteMode && params.isCreator

        val creator = JointAccountMembersUM.MemberUM.Joined(
            id = "creator",
            avatar = JointAccountMembersUM.MemberAvatarUM(
                monogram = "I",
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            name = stringReference("Ivan Zolo"),
            role = stringReference("You • Creator"),
            onInfoClick = ::onMemberInfoClick,
        )

        val freeSlots = List(size = FREE_SLOTS_COUNT) { index ->
            JointAccountMembersUM.MemberUM.FreeSlot(
                id = "slot_$index",
                title = stringReference("Member"),
                status = stringReference("Not invited"),
                canInvite = canInvite,
                onInviteClick = ::onInviteClick,
            )
        }

        return JointAccountMembersUM(
            title = stringReference(if (isInviteMode) "Invite members" else "Members"),
            progress = stringReference("1 of ${FREE_SLOTS_COUNT + 1} joined, including you"),
            shareSafely = JointAccountMembersUM.ShareSafelyUM(
                title = stringReference("Share invites safely"),
                description = stringReference(
                    "Anyone with the link can join and sign. Only share it with people you trust",
                ),
                onClick = ::onShareSafelyClick,
            ),
            members = (listOf(creator) + freeSlots).toImmutableList(),
            otherMembersLabel = if (freeSlots.isNotEmpty()) stringReference("Other members") else null,
            canArchive = isInviteMode,
            onArchiveClick = ::onArchiveClick,
            onCloseClick = ::onCloseClick,
        )
    }

    private fun onShareSafelyClick() {
        // TODO([REDACTED_TASK_KEY]): open the "Sharing safely" info sheet
    }

    private fun onMemberInfoClick() {
        // TODO([REDACTED_TASK_KEY]): open the member card modal with the full owner address
    }

    private fun onInviteClick() {
        // TODO([REDACTED_TASK_KEY]): share the per-slot invite link via the system share sheet
    }

    private fun onArchiveClick() {
        // TODO: archive the account once domain integration lands. The "Account archived" toast is
        //  shown by the screen.
    }

    private fun onCloseClick() {
        router.pop()
    }

    private companion object {
        const val FREE_SLOTS_COUNT = 4
    }
}