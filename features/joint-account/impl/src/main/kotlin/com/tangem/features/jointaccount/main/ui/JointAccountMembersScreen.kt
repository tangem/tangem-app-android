package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberStatusUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotActionUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotStatusUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountMembersScreen(state: JointAccountMembersUM, modifier: Modifier = Modifier) {
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val footerHeight = if (state.footer != null) with(LocalDensity.current) { footerHeightPx.toDp() } else 0.dp

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = { MembersTopBar(topBar = state.topBar) },
        overlay = {
            val footer = state.footer
            if (footer != null) {
                MembersFooter(
                    state = footer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size -> footerHeightPx = size.height },
                )
            }
        },
    ) { contentPadding ->
        MembersContent(state = state, contentPadding = contentPadding, footerHeight = footerHeight)
    }
}

@Composable
private fun MembersFooter(state: JointAccountMembersUM.FooterUM, modifier: Modifier = Modifier) {
    val hint = state.hint
    val button = state.button
    if (button == null) {
        TangemFooter(modifier = modifier) {
            if (hint != null) FooterHint(hint = hint)
        }
        return
    }

    TangemFooter(
        modifier = modifier,
        extraTop = hint?.let { { FooterHint(hint = it) } },
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.joint_account_confirm_shared_address),
            isEnabled = button.isEnabled,
            onClick = button.onClick,
        )
    }
}

@Composable
private fun FooterHint(hint: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = hint.resolveReference(),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun MembersTopBar(topBar: JointAccountMembersUM.TopBarUM, modifier: Modifier = Modifier) {
    when (topBar) {
        is JointAccountMembersUM.TopBarUM.Titled -> TangemTopNavigation(
            modifier = modifier,
            title = topBar.title,
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            onBack = topBar.onBackClick,
        )
        is JointAccountMembersUM.TopBarUM.Dismissible -> TangemTopNavigation(
            modifier = modifier,
            endButton = {
                TangemButton.Close(
                    modifier = Modifier.testTag(JointAccountCreationTestTags.MEMBERS_CLOSE_BUTTON),
                    onClick = topBar.onCloseClick,
                )
            },
        )
    }
}

@Composable
private fun MembersContent(state: JointAccountMembersUM, contentPadding: PaddingValues, footerHeight: Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            // The footer's height already includes the navigation bar inset; the extra 16dp clears its top fade.
            bottom = maxOf(contentPadding.calculateBottomPadding(), footerHeight) + 16.dp,
        ),
    ) {
        val header = state.header
        if (header != null) {
            item(key = "header") { MembersHeader(header = header) }
        } else {
            item(key = "top_spacer") { Spacer(Modifier.height(12.dp)) }
        }

        val summaryCard = state.summaryCard
        if (summaryCard != null) {
            item(key = "summary_card") {
                SummaryCard(
                    state = summaryCard,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                )
            }
        }

        val membersLabel = state.membersLabel
        if (membersLabel != null) {
            item(key = "members_label") { SectionLabel(text = membersLabel) }
        }

        items(items = state.joined, key = { it.id }) { member ->
            JoinedMemberRow(
                member = member,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }

        items(items = state.freeSlots, key = { it.id }) { slot ->
            FreeSlotRow(
                slot = slot,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun MembersHeader(header: JointAccountMembersUM.HeaderUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            text = header.title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        val subtitle = header.subtitle
        if (subtitle != null) {
            SpacerH(8.dp)
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun SummaryCard(state: JointAccountMembersUM.SummaryCardUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.jointAccountCard()) {
        TangemRow(
            divider = true,
            contentLead = TangemRowContentLead.Start,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            startSlot = { AccountIcon(name = state.name, icon = state.icon, size = AccountIconSize.Default) },
            titleSlot = { TangemRowText(text = state.name, role = TangemRowTextRole.Title) },
            subtitleSlot = { TangemRowText(text = state.membersCount, role = TangemRowTextRole.Subtitle) },
        )
        TangemRow(
            verticalAlignment = TangemRowVerticalAlignment.Center,
            onClick = state.onApprovalsInfoClick,
            titleSlot = {
                TangemRowText(
                    text = resourceReference(CoreUiR.string.joint_account_invite_members_required_approvals),
                    role = TangemRowTextRole.Title,
                )
            },
            valueSlot = { ApprovalsValue(approvals = state.approvals) },
        )
    }
}

@Composable
private fun ApprovalsValue(approvals: TextReference) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = approvals.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.ic_info_20,
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
    )
}

@Composable
private fun JoinedMemberRow(member: MemberUM.Joined, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.jointAccountCard(),
        onClick = member.onInfoClick,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            AccountIcon(
                name = stringReference(member.avatar.monogram),
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = member.avatar.color,
                ),
                size = AccountIconSize.ContactDefault,
            )
        },
        titleSlot = { TangemRowText(text = member.name, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = memberRole(member = member), role = TangemRowTextRole.Subtitle) },
        endSlot = { MemberStatusBadge(status = member.status) },
    )
}

@Composable
private fun memberRole(member: MemberUM.Joined): TextReference {
    val role = stringResourceSafe(
        if (member.isCreator) {
            CoreUiR.string.joint_account_invite_members_creator
        } else {
            CoreUiR.string.joint_account_invite_members_member
        },
    )
    return if (member.isYou) {
        val you = stringResourceSafe(CoreUiR.string.joint_account_members_role_you)
        stringReference("$role ${StringsSigns.DOT} $you")
    } else {
        stringReference(role)
    }
}

@Composable
private fun MemberStatusBadge(status: MemberStatusUM) {
    TangemBadge(
        text = status.text,
        status = status.badgeStatus,
        size = TangemBadge.Size.X6,
    )
}

@Composable
private fun FreeSlotRow(slot: MemberUM.FreeSlot, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.jointAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { FreeSlotAvatar() },
        titleSlot = { TangemRowText(text = slot.title, role = TangemRowTextRole.Title) },
        subtitleSlot = { FreeSlotStatus(status = slot.status) },
        endSlot = slot.action?.let { action ->
            { SlotAction(action = action) }
        },
    )
}

@Composable
private fun FreeSlotStatus(status: SlotStatusUM) {
    when (status) {
        is SlotStatusUM.NotInvited -> TangemRowText(
            text = resourceReference(CoreUiR.string.joint_account_invite_members_not_invited),
            role = TangemRowTextRole.Subtitle,
        )
        is SlotStatusUM.WaitingToJoin -> TangemRowText(
            text = resourceReference(CoreUiR.string.joint_account_invite_members_waiting_to_join),
            role = TangemRowTextRole.Subtitle,
        )
        is SlotStatusUM.Invited -> TangemRowText(text = status.label, role = TangemRowTextRole.Subtitle)
        is SlotStatusUM.Expired -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResourceSafe(CoreUiR.string.joint_account_invite_members_invite_expired),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.ic_clock_16,
                tint = TangemTheme.colors3.icon.status.warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SlotAction(action: SlotActionUM) {
    when (action) {
        is SlotActionUM.Invite -> SlotActionButton(
            textRes = CoreUiR.string.joint_account_invite_members_invite,
            onClick = action.onClick,
        )
        is SlotActionUM.ReInvite -> SlotActionButton(
            textRes = CoreUiR.string.joint_account_invite_members_re_invite,
            onClick = action.onClick,
        )
        is SlotActionUM.Loading -> TangemLoader(
            size = TangemLoaderSize.X24,
            color = TangemTheme.colors3.icon.secondary,
        )
    }
}

@Composable
private fun SlotActionButton(textRes: Int, onClick: () -> Unit) {
    TangemButton(
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X9,
        text = resourceReference(textRes),
        onClick = onClick,
    )
}

@Composable
private fun FreeSlotAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.tertiary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.ic_user_24,
            tint = TangemTheme.colors3.icon.tertiary,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Modifier.jointAccountCard(): Modifier = this
    .clip(RoundedCornerShape(24.dp))
    .background(TangemTheme.colors3.bg.secondary)

// region Preview
@Preview(name = "Light", showBackground = true, heightDp = 874)
@Composable
private fun JointAccountMembersScreen_LightPreview(
    @PreviewParameter(JointAccountMembersPreviewProvider::class) state: JointAccountMembersUM,
) {
    TangemThemePreviewRedesign {
        JointAccountMembersScreen(state = state)
    }
}

@Preview(name = "Dark", showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JointAccountMembersScreen_DarkPreview(
    @PreviewParameter(JointAccountMembersPreviewProvider::class) state: JointAccountMembersUM,
) {
    TangemThemePreviewRedesign {
        JointAccountMembersScreen(state = state)
    }
}

/**
 * Mirrors the real phases the screen goes through (see `MembersScreenPhase` in
 * `JointAccountToMembersUMConverter`) — not ad-hoc combinations, so a preview going stale is a signal the
 * converter's phase mapping changed.
 */
private class JointAccountMembersPreviewProvider :
    CollectionPreviewParameterProvider<JointAccountMembersUM>(
        collection = listOf(
            previewState(
                title = "Invite members",
                joinedCount = 1,
                slots = List(size = 4) { PreviewSlot.NotInvited },
                hint = "Invite 4 more members",
            ),
            previewState(
                title = "Invite members",
                joinedCount = 2,
                slots = listOf(PreviewSlot.Invited, PreviewSlot.Expired, PreviewSlot.Loading),
                hint = "Awaiting for 3 members to join",
            ),
            previewState(
                title = "Waiting for members",
                joinedCount = 2,
                slots = List(size = 3) { PreviewSlot.NotInvited },
                canInvite = false,
                hint = "Awaiting for 3 members to join",
            ),
            previewState(
                title = "Invite members",
                joinedCount = 5,
                slots = emptyList(),
                hint = "Everyone has joined",
            ),
            previewState(
                title = "Confirm the account",
                subtitle = "Before using your Joint Account, make sure every member sees exactly the same address.",
                joinedCount = 5,
                slots = emptyList(),
                memberStatus = MemberStatusUM.NotConfirmed,
                isButtonEnabled = true,
            ),
            previewState(
                title = "Confirm the account",
                subtitle = "Before using your Joint Account, make sure every member sees exactly the same address.",
                joinedCount = 5,
                slots = emptyList(),
                memberStatus = MemberStatusUM.Confirmed,
                hint = "4 members still need to confirm",
                hasButton = false,
            ),
            previewState(
                title = "Members",
                joinedCount = 5,
                slots = emptyList(),
                completed = true,
            ),
        ),
    )

private enum class PreviewSlot { NotInvited, Invited, Expired, Loading }

@Suppress("LongParameterList")
private fun previewState(
    title: String,
    joinedCount: Int,
    slots: List<PreviewSlot>,
    subtitle: String? = null,
    canInvite: Boolean = true,
    memberStatus: MemberStatusUM = MemberStatusUM.Joined,
    hint: String? = null,
    isButtonEnabled: Boolean = false,
    hasButton: Boolean = true,
    completed: Boolean = false,
): JointAccountMembersUM {
    val joined = previewJoined(joinedCount = joinedCount, ownStatus = memberStatus)
    val freeSlots = previewSlots(slots = slots, joinedCount = joinedCount, canInvite = canInvite)
    return JointAccountMembersUM(
        topBar = if (completed) {
            JointAccountMembersUM.TopBarUM.Titled(title = stringReference(title), onBackClick = {})
        } else {
            JointAccountMembersUM.TopBarUM.Dismissible(onCloseClick = {})
        },
        header = if (completed) {
            null
        } else {
            JointAccountMembersUM.HeaderUM(title = stringReference(title), subtitle = subtitle?.let(::stringReference))
        },
        summaryCard = JointAccountMembersUM.SummaryCardUM(
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.CandyGrapeFizz,
            ),
            name = stringReference("Family savings"),
            membersCount = stringReference("5 members"),
            approvals = stringReference("Any 2 of 5"),
            onApprovalsInfoClick = {},
        ),
        joined = joined,
        freeSlots = freeSlots,
        membersLabel = if (joined.isNotEmpty()) stringReference("Members") else null,
        footer = if (completed) {
            null
        } else {
            previewFooter(
                hint = hint,
                hasButton = hasButton,
                isEnabled = isButtonEnabled,
            )
        },
    )
}

private fun previewJoined(joinedCount: Int, ownStatus: MemberStatusUM) = List(size = joinedCount) { index ->
    MemberUM.Joined(
        id = "member_$index",
        avatar = JointAccountMembersUM.MemberAvatarUM(
            monogram = "I",
            color = CryptoPortfolioIcon.Color.entries[index % CryptoPortfolioIcon.Color.entries.size],
        ),
        name = stringReference(if (index == 0) "Ivan Zolo" else "Member $index"),
        isYou = index == 0,
        isCreator = index == 0,
        status = when {
            ownStatus == MemberStatusUM.Joined -> MemberStatusUM.Joined
            index == 0 -> ownStatus
            else -> MemberStatusUM.NotConfirmed
        },
        onInfoClick = {},
    )
}.toImmutableList()

private fun previewSlots(slots: List<PreviewSlot>, joinedCount: Int, canInvite: Boolean) = slots
    .mapIndexed { index, slot ->
        MemberUM.FreeSlot(
            id = "slot_$index",
            title = stringReference("Member ${joinedCount + index}"),
            status = when (slot) {
                PreviewSlot.NotInvited -> if (canInvite) SlotStatusUM.NotInvited else SlotStatusUM.WaitingToJoin
                PreviewSlot.Invited -> SlotStatusUM.Invited(label = stringReference("Until tomorrow, 13:15"))
                PreviewSlot.Expired -> SlotStatusUM.Expired
                PreviewSlot.Loading -> SlotStatusUM.Invited(label = stringReference("Invited"))
            },
            action = when {
                !canInvite -> null
                slot == PreviewSlot.Loading -> SlotActionUM.Loading
                slot == PreviewSlot.NotInvited -> SlotActionUM.Invite(onClick = {})
                else -> SlotActionUM.ReInvite(onClick = {})
            },
        )
    }
    .toImmutableList()

private fun previewFooter(hint: String?, hasButton: Boolean, isEnabled: Boolean) = JointAccountMembersUM.FooterUM(
    hint = hint?.let(::stringReference),
    button = if (hasButton) {
        JointAccountMembersUM.ConfirmButtonUM(
            isEnabled = isEnabled,
            onClick = {},
        )
    } else {
        null
    },
)
// endregion