package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dots_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24_filled
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberUM
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountMembersScreen(state: JointAccountMembersUM, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val archivedToastText = stringResourceSafe(CoreUiR.string.account_archive_success_message)

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            MembersTopBar(
                canArchive = state.canArchive,
                onArchiveClick = {
                    state.onArchiveClick()
                    coroutineScope.launch { snackbarHostState.showSnackbar(archivedToastText) }
                },
                onCloseClick = state.onCloseClick,
            )
        },
    ) { contentPadding ->
        MembersContent(state = state, contentPadding = contentPadding)

        val activation = state.activation
        if (activation != null) {
            ActivateAccountFooter(
                onActivateClick = activation.onActivateClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    // The snackbar rises above the activation footer instead of covering its CTA
                    bottom = contentPadding.calculateBottomPadding() +
                        if (activation != null) ACTIVATION_FOOTER_HEIGHT else 0.dp,
                ),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = TangemTheme.colors3.bg.inverse,
                contentColor = TangemTheme.colors3.text.inverse.primary,
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
private fun MembersTopBar(canArchive: Boolean, onArchiveClick: () -> Unit, onCloseClick: () -> Unit) {
    TangemTopNavigation(
        endButtonsGroup = if (canArchive) {
            { MembersMenuButton(onArchiveClick = onArchiveClick) }
        } else {
            null
        },
        endButton = { TangemButton.Close(onClick = onCloseClick) },
    )
}

@Composable
private fun MembersMenuButton(onArchiveClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X11,
            iconStart = TangemIconUM.Icon(Icons.ic_dots_horizontal_20),
            contentDescription = stringResourceSafe(CoreUiR.string.common_more),
            onClick = { isExpanded = true },
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = TangemTheme.colors3.bg.secondary,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResourceSafe(CoreUiR.string.account_details_archive),
                        color = TangemTheme.colors3.text.status.error,
                        style = TangemTheme.typography3.body.medium,
                    )
                },
                colors = MenuDefaults.itemColors(),
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
            // The activation footer overlays the list bottom — reserve its height so the last row scrolls above it
            bottom = contentPadding.calculateBottomPadding() +
                if (state.activation != null) ACTIVATION_FOOTER_HEIGHT + 16.dp else 16.dp,
        ),
    ) {
        item(key = "header") { MembersHeader(title = state.title, progress = state.progress) }

        item(key = "share_safely") {
            ShareSafelyBanner(
                state = state.shareSafely,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            )
        }

        val (joined, freeSlots) = state.members.partitionMembers()

        items(items = joined, key = { it.id }) { member ->
            JoinedMemberRow(
                member = member,
                modifier = Modifier.padding(horizontal = 16.dp),
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

        items(items = freeSlots, key = { it.id }) { slot ->
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
            style = TangemTheme.typography3.body.medium,
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
            painter = painterResource(CoreUiR.drawable.ic_user_24),
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

private fun List<MemberUM>.partitionMembers(): Pair<List<MemberUM.Joined>, List<MemberUM.FreeSlot>> {
    val joined = filterIsInstance<MemberUM.Joined>()
    val freeSlots = filterIsInstance<MemberUM.FreeSlot>()
    return joined to freeSlots
}

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

private class JointAccountMembersPreviewProvider :
    CollectionPreviewParameterProvider<JointAccountMembersUM>(
        collection = listOf(
            previewState(title = "Invite members", canInvite = true, canArchive = true),
            previewState(title = "Invite members", canInvite = false, canArchive = false),
            previewState(title = "Members", canInvite = false, canArchive = false),
            previewState(title = "Invite members", canInvite = false, canArchive = true, canActivate = true),
        ),
    )

private fun previewState(
    title: String,
    canInvite: Boolean,
    canArchive: Boolean,
    canActivate: Boolean = false,
): JointAccountMembersUM {
    val creator = MemberUM.Joined(
        id = "creator",
        avatar = JointAccountMembersUM.MemberAvatarUM(monogram = "I", color = CryptoPortfolioIcon.Color.Azure),
        name = stringReference("Ivan Zolo"),
        role = stringReference("You • Creator"),
        onInfoClick = {},
    )
    val freeSlots = List(size = 4) { index ->
        MemberUM.FreeSlot(
            id = "slot_$index",
            title = stringReference("Member"),
            status = stringReference("Not invited"),
            canInvite = canInvite,
            onInviteClick = {},
        )
    }
    return JointAccountMembersUM(
        title = stringReference(title),
        progress = stringReference("1 of 5 joined, including you"),
        shareSafely = JointAccountMembersUM.ShareSafelyUM(
            title = stringReference("Share invites safely"),
            description = stringReference(
                "Anyone with the link can join and sign. Only share it with people you trust",
            ),
            onClick = {},
        ),
        members = (listOf(creator) + freeSlots).toImmutableList(),
        otherMembersLabel = stringReference("Other members"),
        canArchive = canArchive,
        activation = if (canActivate) {
            JointAccountMembersUM.ActivationUM(onActivateClick = {}, confirmation = null)
        } else {
            null
        },
        onArchiveClick = {},
        onCloseClick = {},
    )
}
// endregion