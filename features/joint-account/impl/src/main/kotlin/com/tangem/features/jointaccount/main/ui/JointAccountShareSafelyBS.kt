package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_eye_24
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountShareSafelyBS(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_sheet_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = onDismiss,
            )
        },
        content = {
            Content(modifier = modifier)
        },
        footer = {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.common_got_it),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun Content(modifier: Modifier = Modifier) {
    val bottomInset = LocalTangemBottomSheetContentBottomInset.current
    val scrollState = rememberScrollState()
    val contentScrollable = LocalBottomSheetContentScrollable.current

    if (contentScrollable != null) {
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.canScrollForward || scrollState.canScrollBackward }
                .collect { isScrollable -> contentScrollable.value = isScrollable }
        }
    }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(scrollState)) {
        ShareSafelyPointRow(
            icon = Icons.ic_lightning_24,
            title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_first_point_title),
            description = resourceReference(
                CoreUiR.string.joint_account_invite_members_safety_first_point_subtitle,
            ),
        )
        ShareSafelyPointRow(
            icon = Icons.ic_eye_24,
            title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_second_point_title),
            description = resourceReference(
                CoreUiR.string.joint_account_invite_members_safety_second_point_subtitle,
            ),
        )
        ShareSafelyPointRow(
            icon = Icons.ic_info_24,
            title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_third_point_title),
            description = resourceReference(
                CoreUiR.string.joint_account_invite_members_safety_third_point_subtitle,
            ),
        )
        Spacer(modifier = Modifier.height(bottomInset))
    }
}

@Composable
private fun ShareSafelyPointRow(
    icon: ImageVector,
    title: TextReference,
    description: TextReference,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        includeInnerPaddings = false,
        startSlot = { ShareSafelyPointIcon(icon = icon) },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(text = description, role = TangemRowTextRole.Subtitle, maxLines = Int.MAX_VALUE)
        },
    )
}

@Composable
private fun ShareSafelyPointIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.status.infoSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            tint = TangemTheme.colors3.icon.status.info,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountShareSafely() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            TangemTopNavigation(
                title = stringReference("Sharing safely"),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = {},
            )
            Content()
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.common_got_it),
                onClick = {},
            )
        }
    }
}
// endregion