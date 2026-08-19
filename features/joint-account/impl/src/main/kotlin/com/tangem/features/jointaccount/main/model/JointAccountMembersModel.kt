package com.tangem.features.jointaccount.main.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

// TODO([REDACTED_TASK_KEY]): visible texts are hardcoded English literals while the screen runs on stubbed
//  state. Replace with resource references once the invite-members strings land in Lokalise.
@Stable
@ModelScoped
internal class JointAccountMembersModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<JointAccountMembersComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<MemberCardConfig> = SlotNavigation()

    val uiState: StateFlow<JointAccountMembersUM>
        field = MutableStateFlow(createStubState())

    private fun createStubState(): JointAccountMembersUM {
        val isInviteMode = params.mode == JointAccountMembersComponent.Mode.Invite
        val canInvite = isInviteMode && params.isCreator

        val creatorAvatar = JointAccountMembersUM.MemberAvatarUM(
            monogram = "I",
            color = CryptoPortfolioIcon.Color.Azure,
        )
        val creator = JointAccountMembersUM.MemberUM.Joined(
            id = "creator",
            avatar = creatorAvatar,
            name = stringReference("Ivan Zolo"),
            role = stringReference("You • Creator"),
            onInfoClick = {
                onMemberInfoClick(
                    avatar = creatorAvatar,
                    name = stringReference("Ivan Zolo"),
                    address = STUB_MEMBER_ADDRESS,
                )
            },
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
            // TODO([REDACTED_TASK_KEY]): on the stubbed state the action is gated by the creator flag only; the real
            //  gate — every slot is filled and the account is `confirming` — arrives with domain integration
            activation = if (canInvite) {
                JointAccountMembersUM.ActivationUM(
                    onActivateClick = ::onActivateClick,
                    confirmation = null,
                )
            } else {
                null
            },
            onArchiveClick = ::onArchiveClick,
            onCloseClick = ::onCloseClick,
        )
    }

    private fun onActivateClick() {
        updateActivationConfirmation(
            confirmation = JointAccountMembersUM.ActivationUM.ConfirmationUM(
                onConfirmClick = ::onConfirmActivationClick,
                onCancelClick = ::onCancelActivationClick,
            ),
        )
    }

    private fun onConfirmActivationClick() {
        // TODO([REDACTED_TASK_KEY]): run the activation orchestrator (one-tap payload signing) once it lands
        updateActivationConfirmation(confirmation = null)
    }

    private fun onCancelActivationClick() {
        updateActivationConfirmation(confirmation = null)
    }

    private fun updateActivationConfirmation(confirmation: JointAccountMembersUM.ActivationUM.ConfirmationUM?) {
        uiState.update { state ->
            state.copy(activation = state.activation?.copy(confirmation = confirmation))
        }
    }

    private fun onShareSafelyClick() {
        // TODO([REDACTED_TASK_KEY]): open the "Sharing safely" info sheet
    }

    private fun onMemberInfoClick(avatar: JointAccountMembersUM.MemberAvatarUM, name: TextReference, address: String) {
        bottomSheetNavigation.activate(
            MemberCardConfig(avatar = avatar, name = name, address = address),
        )
    }

    fun onCopyAddressClick(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.wallet_notification_address_copied),
                startIconId = CoreUiR.drawable.ic_check_24,
            ),
        )
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
        const val STUB_MEMBER_ADDRESS = "0xBef7B368aac4e6752A9cE0xBef7B36A9cE"
    }
}