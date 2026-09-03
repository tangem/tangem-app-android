package com.tangem.features.jointaccount.main.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.deeplink.JointAccountInviteLinkBuilder
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.utils.StringsSigns
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class JointAccountMembersModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val shareManager: ShareManager,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val getJointAccountInvitesUseCase: GetJointAccountInvitesUseCase,
) : Model() {

    private val params = paramsContainer.require<JointAccountMembersComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<MemberCardConfig> = SlotNavigation()
    val shareSafelyNavigation: SlotNavigation<Unit> = SlotNavigation()

    val uiState: StateFlow<JointAccountMembersUM>
        field = MutableStateFlow(createInitialState())

    private val isInviteMode: Boolean
        get() = params.mode == JointAccountMembersComponent.Mode.Invite

    init {
        observeAccount()
    }

    private fun observeAccount() {
        jointAccountListSupplier(userWalletId = params.userWalletId)
            .map { accounts ->
                accounts.firstOrNull { params.cryptoAccountId == null || it.cryptoAccountId == params.cryptoAccountId }
            }
            .distinctUntilChanged()
            .map { account ->
                account?.let { found ->
                    found to getJointAccountInvitesUseCase(
                        userWalletId = params.userWalletId,
                        cryptoAccountId = found.cryptoAccountId,
                    )
                }
            }
            // The account can leave the supplier (refreshed away, removed): stale members must not survive it
            .onEach { accountWithInvites ->
                uiState.update {
                    accountWithInvites
                        ?.let { (account, invites) -> buildState(account = account, invites = invites) }
                        ?: createInitialState()
                }
            }
            .launchIn(modelScope)
    }

    private fun createInitialState(): JointAccountMembersUM = JointAccountMembersUM(
        title = title(),
        progress = TextReference.EMPTY,
        shareSafely = shareSafelyBlock(),
        members = persistentListOf(),
        otherMembersLabel = null,
        canArchive = false,
        activation = null,
        onArchiveClick = ::onArchiveClick,
        onCloseClick = ::onCloseClick,
    )

    private fun buildState(account: JointAccount, invites: List<String>): JointAccountMembersUM {
        val isThisDeviceCreator = invites.isNotEmpty()
        val joined = account.members.mapIndexed { index, member ->
            joinedMember(index = index, member = member, isThisDeviceCreator = isThisDeviceCreator)
        }
        val redeemedInvites = account.members
            .mapNotNull { member -> member.inviteId?.lowercase() }
            .toSet()
        val remainingInvites = invites.filterNot { invite -> invite.lowercase() in redeemedInvites }
        val freeSlots = List(size = (account.membersCount - account.members.size).coerceAtLeast(minimumValue = 0)) {
            freeSlot(index = it, invite = remainingInvites.getOrNull(it))
        }

        return JointAccountMembersUM(
            title = title(),
            progress = resourceReference(
                id = if (isInviteMode) {
                    CoreUiR.string.joint_account_invite_members_subtitle
                } else {
                    CoreUiR.string.joint_account_members_progress
                },
                formatArgs = wrappedList(account.members.size, account.membersCount),
            ),
            shareSafely = shareSafelyBlock(),
            members = (joined + freeSlots).toImmutableList(),
            otherMembersLabel = if (freeSlots.isNotEmpty()) {
                resourceReference(CoreUiR.string.joint_account_invite_members_other_members)
            } else {
                null
            },
            canArchive = isInviteMode,
            activation = activation(account = account, isCreator = isThisDeviceCreator),
            onArchiveClick = ::onArchiveClick,
            onCloseClick = ::onCloseClick,
        )
    }

    private fun joinedMember(
        index: Int,
        member: JointAccount.Member,
        isThisDeviceCreator: Boolean,
    ): JointAccountMembersUM.MemberUM.Joined {
        val avatar = avatar(member = member, index = index)
        val name = stringReference(member.name)

        return JointAccountMembersUM.MemberUM.Joined(
            id = member.address,
            avatar = avatar,
            name = name,
            role = role(member = member, isThisDeviceCreator = isThisDeviceCreator),
            onInfoClick = { onMemberInfoClick(avatar = avatar, name = name, address = member.address) },
        )
    }

    private fun role(member: JointAccount.Member, isThisDeviceCreator: Boolean): TextReference {
        val isCreator = member.role == JointAccount.Role.CREATOR

        return when {
            isCreator && isThisDeviceCreator -> combinedReference(
                resourceReference(CoreUiR.string.joint_account_members_role_you),
                stringReference(" ${StringsSigns.DOT} "),
                resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            )
            isCreator -> resourceReference(CoreUiR.string.joint_account_invite_members_creator)
            else -> resourceReference(CoreUiR.string.joint_account_members_role_joined)
        }
    }

    private fun avatar(member: JointAccount.Member, index: Int): JointAccountMembersUM.MemberAvatarUM {
        val colors = CryptoPortfolioIcon.Color.entries

        return JointAccountMembersUM.MemberAvatarUM(
            monogram = member.name.take(n = 1).uppercase(),
            color = colors[index % colors.size],
        )
    }

    private fun freeSlot(index: Int, invite: String?): JointAccountMembersUM.MemberUM.FreeSlot {
        return JointAccountMembersUM.MemberUM.FreeSlot(
            id = "slot_$index",
            title = resourceReference(CoreUiR.string.joint_account_invite_members_new_member),
            status = resourceReference(CoreUiR.string.joint_account_invite_members_not_invited),
            canInvite = isInviteMode && invite != null,
            onInviteClick = { invite?.let(::shareInvite) },
        )
    }

    /** The slot's own invite: a link issued for one slot cannot be redeemed for another */
    private fun shareInvite(inviteId: String) {
        shareManager.shareText(text = JointAccountInviteLinkBuilder.build(inviteId = inviteId))
    }

    private fun activation(account: JointAccount, isCreator: Boolean): JointAccountMembersUM.ActivationUM? {
        val canActivate = isInviteMode && isCreator && account.status == JointAccount.Status.CONFIRMING

        if (!canActivate) return null

        return JointAccountMembersUM.ActivationUM(
            onActivateClick = ::onActivateClick,
            confirmation = null,
        )
    }

    private fun title(): TextReference = if (isInviteMode) {
        resourceReference(CoreUiR.string.joint_account_invite_members_title)
    } else {
        resourceReference(CoreUiR.string.joint_account_members_title)
    }

    private fun shareSafelyBlock() = JointAccountMembersUM.ShareSafelyUM(
        title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_title),
        description = resourceReference(CoreUiR.string.joint_account_invite_members_safety_subtitle),
        onClick = ::onShareSafelyClick,
    )

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
        shareSafelyNavigation.activate(Unit)
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

    private fun onArchiveClick() {
        // TODO: archive the account once domain integration lands. The "Account archived" toast is
        //  shown by the screen.
    }

    private fun onCloseClick() {
        router.pop()
    }
}