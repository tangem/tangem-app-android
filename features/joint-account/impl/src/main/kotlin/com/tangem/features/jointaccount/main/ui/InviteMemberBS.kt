package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_share_android_24
import com.tangem.core.ui.res.generated.icons.ic_success_20
import com.tangem.core.ui.res.generated.icons.ic_user_plus_20
import com.tangem.core.ui.res.generated.icons.ic_users_20
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun InviteMemberBS(message: TextReference, onShareClick: (message: String) -> Unit, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(
                title = resourceReference(CoreUiR.string.joint_account_invite_member_title),
                onClose = onDismiss,
            )
        },
        content = {
            val resolvedMessage = message.resolveReference()
            InviteMemberContent(message = resolvedMessage, onShareClick = { onShareClick(resolvedMessage) })
        },
    )
}

@Composable
private fun ColumnScope.InviteMemberContent(message: String, onShareClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(weight = 1f, fill = false)
            .verticalScroll(rememberScrollState()),
    ) {
        InvitePoints()
        InviteMessageCard(message = message)
    }
    TangemFooter(
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeEnabled = false,
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_share),
            iconEnd = TangemIconUM.Icon(Icons.ic_share_android_24),
            onClick = onShareClick,
        )
    }
}

@Composable
private fun InvitePoints() {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        InvitePointRow(
            icon = Icons.ic_user_plus_20,
            title = resourceReference(CoreUiR.string.joint_account_invite_member_one_link_title),
            description = resourceReference(CoreUiR.string.joint_account_invite_member_one_link_subtitle),
        )
        InvitePointRow(
            icon = Icons.ic_users_20,
            title = resourceReference(CoreUiR.string.joint_account_invite_member_joins_first_title),
            description = resourceReference(CoreUiR.string.joint_account_invite_member_joins_first_subtitle),
        )
        InvitePointRow(
            icon = Icons.ic_success_20,
            title = resourceReference(CoreUiR.string.joint_account_invite_member_verifies_title),
            description = resourceReference(CoreUiR.string.joint_account_invite_member_verifies_subtitle),
        )
    }
}

@Composable
private fun InvitePointRow(icon: ImageVector, title: TextReference, description: TextReference) {
    TangemRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        includeInnerPaddings = false,
        startSlot = {
            Icon(
                imageVector = icon,
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(text = description, role = TangemRowTextRole.Subtitle, maxLines = Int.MAX_VALUE)
        },
    )
}

/** The link sits on the message's last line and is cut by a fade rather than wrapped, as it can't be read anyway */
@Composable
private fun InviteMessageCard(message: String) {
    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.transparent.onMaterial.primary)
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResourceSafe(CoreUiR.string.joint_account_invite_member_message_label),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Column {
            Text(
                text = message.substringBeforeLast(delimiter = '\n'),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            if (message.contains('\n')) {
                InviteLink(link = message.substringAfterLast(delimiter = '\n'))
            }
        }
    }
}

@Composable
private fun InviteLink(link: String) {
    var isOverflowing by remember { mutableStateOf(false) }

    Text(
        text = link,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { isOverflowing = it.hasVisualOverflow },
        modifier = if (isOverflowing) Modifier.fadeOutEnd() else Modifier,
    )
}

private fun Modifier.fadeOutEnd(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = size.width - LINK_FADE_WIDTH.toPx(),
                endX = size.width,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private val LINK_FADE_WIDTH = 40.dp

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_InviteMember() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            InviteMemberContent(
                message = "Join my joint account “Family savings” in Tangem\n" +
                    "https://tangem.com/joint-account/join/0EB4F7E48E9DC3F0EB4F7E48E9DC3F",
                onShareClick = {},
            )
        }
    }
}
// endregion