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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuItem
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberUM
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountMembersScreen(state: JointAccountMembersUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = { MembersTopBar(topBar = state.topBar) },
    ) { contentPadding ->
        MembersContent(state = state, contentPadding = contentPadding)

        val onActivateClick = state.onActivateClick
        if (onActivateClick != null) {
            ActivateAccountFooter(
                onActivateClick = onActivateClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }
}

/** The "Activate account" button (48dp) plus its vertical paddings (12dp + 12dp). */
private val ACTIVATION_FOOTER_HEIGHT = 72.dp

@Composable
private fun ActivateAccountFooter(onActivateClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        variant = TangemButton.Variant.Primary,
        size = TangemButton.Size.X12,
        text = resourceReference(CoreUiR.string.joint_account_activation_account_button_title),
        onClick = onActivateClick,
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
            endButtonsGroup = if (topBar.canArchive) {
                { MembersMenuButton(onArchiveClick = topBar.onArchiveClick) }
            } else {
                null
            },
            endButton = { TangemButton.Close(onClick = topBar.onCloseClick) },
        )
    }
}

@Composable
private fun MembersMenuButton(onArchiveClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        // A button group entry is X9: the group pill adds its own 4dp, which levels it with the 44dp close button
        TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X9,
            iconStart = TangemIconUM.Icon(Icons.ic_dots_horizontal_20),
            contentDescription = stringResourceSafe(CoreUiR.string.common_more),
            onClick = { isExpanded = true },
        )
        TangemContextMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            TangemContextMenuItem(
                title = resourceReference(CoreUiR.string.account_details_archive),
                isDestructive = true,
                onClick = {
                    isExpanded = false
                    onArchiveClick()
                },
            )
        }
    }
}

@Composable
private fun MembersContent(state: JointAccountMembersUM, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() +
                if (state.onActivateClick != null) ACTIVATION_FOOTER_HEIGHT + 16.dp else 16.dp,
        ),
    ) {
        val header = state.header
        if (header != null) {
            item(key = "header") { MembersHeader(title = header.title, progress = header.progress) }
        } else {
            item(key = "top_spacer") { Spacer(Modifier.height(12.dp)) }
        }

        val shareSafely = state.shareSafely
        if (shareSafely != null) {
            item(key = "share_safely") {
                ShareSafelyBanner(
                    state = shareSafely,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
            }
        }

        items(items = state.joined, key = { it.id }) { member ->
            JoinedMemberRow(
                member = member,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }

        val otherMembersLabel = state.otherMembersLabel
        if (otherMembersLabel != null) {
            item(key = "other_members") {
                Text(
                    text = otherMembersLabel.resolveReference(),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                    modifier = Modifier.padding(top = 28.dp, bottom = 16.dp, start = 16.dp),
                )
            }
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
private fun MembersHeader(title: TextReference, progress: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = progress.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun ShareSafelyBanner(state: JointAccountMembersUM.ShareSafelyUM, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        modifier = modifier,
        variant = TangemMessageBanner.Variant.Info,
        title = state.title,
        description = state.description,
        onClick = state.onClick,
        slotStart = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_shield_checkmark_24_filled,
                    tintReference = { TangemTheme.colors3.icon.brand },
                ),
                modifier = Modifier.size(24.dp),
            )
        },
        slotEnd = {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_chevron_right_24),
                tint = TangemTheme.colors3.icon.tertiary,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
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
        subtitleSlot = { TangemRowText(text = member.role, role = TangemRowTextRole.Subtitle) },
        endSlot = {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X9,
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_info_24),
                contentDescription = stringResourceSafe(CoreUiR.string.joint_account_invite_members_member_info),
                onClick = member.onInfoClick,
            )
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
        endSlot = {
            if (slot.canInvite) {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X9,
                    text = resourceReference(CoreUiR.string.joint_account_invite_members_invite),
                    onClick = slot.onInviteClick,
                )
            }
        },
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
 * Mirrors the four real phases the screen goes through (see `MembersScreenPhase` in
 * `JointAccountToMembersUMConverter`) — not ad-hoc combinations, so a preview going stale is a signal the
 * converter's phase mapping changed.
 */
private class JointAccountMembersPreviewProvider :
    CollectionPreviewParameterProvider<JointAccountMembersUM>(
        collection = listOf(
            previewState(
                title = "Invite members",
                progress = "1 of 5 joined, including you",
                joinedCount = 1,
                canInvite = true,
                canArchive = true,
            ),
            previewState(
                title = "Waiting for members",
                progress = "2 of 5 joined, including you",
                joinedCount = 2,
                canInvite = false,
                canArchive = false,
                showShareSafely = false,
            ),
            previewState(
                title = "Invite members",
                progress = "5 of 5 joined, including you",
                joinedCount = 5,
                canInvite = false,
                canArchive = true,
                canActivate = true,
            ),
            previewState(
                title = "Members",
                progress = "5 of 5 joined, including you",
                joinedCount = 5,
                canInvite = false,
                canArchive = false,
                showShareSafely = false,
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
    canArchive: Boolean,
    canActivate: Boolean = false,
    showShareSafely: Boolean = true,
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
            role = stringReference(if (index == 0) "You • Creator" else "Joined"),
            onInfoClick = {},
        )
    }
    val freeSlots = List(size = (5 - joinedCount).coerceAtLeast(0)) { index ->
        MemberUM.FreeSlot(
            id = "slot_$index",
            title = stringReference("Member"),
            status = stringReference("Not invited"),
            canInvite = canInvite,
            onInviteClick = {},
        )
    }
    return JointAccountMembersUM(
        topBar = if (completed) {
            JointAccountMembersUM.TopBarUM.Titled(title = stringReference(title), onBackClick = {})
        } else {
            JointAccountMembersUM.TopBarUM.Dismissible(canArchive = canArchive, onArchiveClick = {}, onCloseClick = {})
        },
        header = if (completed) {
            null
        } else {
            JointAccountMembersUM.HeaderUM(title = stringReference(title), progress = stringReference(progress))
        },
        shareSafely = if (showShareSafely) {
            JointAccountMembersUM.ShareSafelyUM(
                title = stringReference("Share invites safely"),
                description = stringReference(
                    "Anyone with the link can join and sign. Only share it with people you trust",
                ),
                onClick = {},
            )
        } else {
            null
        },
        joined = joined.toImmutableList(),
        freeSlots = freeSlots.toImmutableList(),
        otherMembersLabel = if (freeSlots.isNotEmpty()) stringReference("Other members") else null,
        onActivateClick = if (canActivate) {
            {}
        } else {
            null
        },
    )
}
// endregion