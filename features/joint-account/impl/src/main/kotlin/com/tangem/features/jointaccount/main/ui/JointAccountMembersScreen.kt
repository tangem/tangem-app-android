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
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
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
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberUM
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
    TangemFooter(
        modifier = modifier,
        extraTop = hint?.let {
            {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = it.resolveReference(),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.joint_account_invite_members_verify_account),
            iconEnd = state.button.walletIconRes?.let { TangemIconUM.Icon(iconRes = it) },
            isEnabled = state.button.isEnabled,
            isLoading = state.button.isLoading,
            onClick = state.button.onClick,
        )
    }
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
            item(key = "header") { MembersHeader(title = header.title) }
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

        val waitingLabel = state.waitingLabel
        if (waitingLabel != null) {
            item(key = "waiting_label") { SectionLabel(text = waitingLabel) }
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
private fun MembersHeader(title: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.heading.medium,
        color = TangemTheme.colors3.text.primary,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
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
            subtitleSlot = { TangemRowText(text = state.progress, role = TangemRowTextRole.Subtitle) },
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
        titleSlot = {
            TangemRowText(
                text = member.name,
                role = TangemRowTextRole.Title,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
            if (member.isYou) {
                Spacer(Modifier.width(4.dp))
                TangemBadge(
                    text = resourceReference(CoreUiR.string.joint_account_members_role_you),
                    size = TangemBadge.Size.X4,
                )
            }
        },
        subtitleSlot = { TangemRowText(text = member.status, role = TangemRowTextRole.Subtitle) },
        endSlot = if (member.isCreator) {
            {
                TangemBadge(
                    text = resourceReference(CoreUiR.string.joint_account_invite_members_creator),
                    status = TangemBadge.Status.Info,
                    size = TangemBadge.Size.X6,
                )
            }
        } else {
            null
        },
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
        subtitleSlot = { TangemRowText(text = slot.status, role = TangemRowTextRole.Subtitle) },
        endSlot = slot.action?.let { action ->
            { SlotActionButton(action = action) }
        },
    )
}

@Composable
private fun SlotActionButton(action: JointAccountMembersUM.SlotActionUM) {
    val text = when (action) {
        is JointAccountMembersUM.SlotActionUM.Invite -> CoreUiR.string.joint_account_invite_members_invite
        is JointAccountMembersUM.SlotActionUM.Resend -> CoreUiR.string.joint_account_invite_members_resend
    }
    TangemButton(
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X9,
        text = resourceReference(text),
        onClick = action.onClick,
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
                progress = "1 of 5 members joined",
                joinedCount = 1,
                canInvite = true,
            ),
            previewState(
                title = "Invite members",
                progress = "3 of 5 members joined",
                joinedCount = 3,
                canInvite = true,
                isFirstSlotResend = true,
            ),
            previewState(
                title = "Waiting for members",
                progress = "2 of 5 members joined",
                joinedCount = 2,
                canInvite = false,
            ),
            previewState(
                title = "Invite members",
                progress = "5 of 5 members joined",
                joinedCount = 5,
                canInvite = false,
                canActivate = true,
            ),
            previewState(
                title = "Members",
                progress = "5 of 5 members joined",
                joinedCount = 5,
                canInvite = false,
                completed = true,
            ),
        ),
    )

@Suppress("LongParameterList")
private fun previewState(
    title: String,
    progress: String,
    joinedCount: Int,
    canInvite: Boolean,
    isFirstSlotResend: Boolean = false,
    canActivate: Boolean = false,
    completed: Boolean = false,
): JointAccountMembersUM {
    val joined = List(size = joinedCount) { index ->
        MemberUM.Joined(
            id = "member_$index",
            avatar = JointAccountMembersUM.MemberAvatarUM(
                monogram = "I",
                color = CryptoPortfolioIcon.Color.entries[index % CryptoPortfolioIcon.Color.entries.size],
            ),
            name = stringReference(if (index == 0) "Ivan Zolo" else "Member $index"),
            isYou = index == 0,
            isCreator = index == 0,
            status = stringReference("Joined"),
            onInfoClick = {},
        )
    }
    val freeSlots = List(size = (5 - joinedCount).coerceAtLeast(0)) { index ->
        MemberUM.FreeSlot(
            id = "slot_$index",
            title = stringReference("Member ${joinedCount + index}"),
            status = stringReference(if (isFirstSlotResend && index == 0) "Invited" else "Not invited"),
            action = when {
                !canInvite -> null
                isFirstSlotResend && index == 0 -> JointAccountMembersUM.SlotActionUM.Resend(onClick = {})
                else -> JointAccountMembersUM.SlotActionUM.Invite(onClick = {})
            },
        )
    }
    return JointAccountMembersUM(
        topBar = if (completed) {
            JointAccountMembersUM.TopBarUM.Titled(title = stringReference(title), onBackClick = {})
        } else {
            JointAccountMembersUM.TopBarUM.Dismissible(onCloseClick = {})
        },
        header = if (completed) {
            null
        } else {
            JointAccountMembersUM.HeaderUM(title = stringReference(title))
        },
        summaryCard = JointAccountMembersUM.SummaryCardUM(
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.CandyGrapeFizz,
            ),
            name = stringReference("Family savings"),
            progress = stringReference(progress),
            approvals = stringReference("Any 2 of 5"),
            onApprovalsInfoClick = {},
        ),
        joined = joined.toImmutableList(),
        freeSlots = freeSlots.toImmutableList(),
        membersLabel = if (joined.isNotEmpty()) stringReference("Members") else null,
        waitingLabel = if (freeSlots.isNotEmpty()) stringReference("Waiting to join") else null,
        footer = if (completed) {
            null
        } else {
            JointAccountMembersUM.FooterUM(
                hint = if (canActivate) null else stringReference("${freeSlots.size} members still need to join"),
                button = JointAccountMembersUM.VerifyButtonUM(
                    isEnabled = canActivate,
                    isLoading = false,
                    walletIconRes = CoreUiR.drawable.ic_tangem_24,
                    onClick = {},
                ),
            )
        },
    )
}
// endregion