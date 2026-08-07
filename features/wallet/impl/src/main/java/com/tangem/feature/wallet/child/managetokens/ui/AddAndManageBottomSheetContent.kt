package com.tangem.feature.wallet.child.managetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_24
import com.tangem.feature.wallet.impl.R

@Composable
internal fun AddAndManageBottomSheetContent(
    onAddTokensClick: () -> Unit,
    shouldShowOrganizeButton: Boolean,
    onOrganizeTokensClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.tertiary,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.main_add_and_manage_tokens),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = onDismiss,
            )
        },
        content = {
            AddAndManageContent(
                modifier = Modifier.padding(bottom = 16.dp),
                onAddTokensClick = onAddTokensClick,
                shouldShowOrganizeButton = shouldShowOrganizeButton,
                onOrganizeTokensClick = onOrganizeTokensClick,
            )
        },
    )
}

@Composable
private fun AddAndManageContent(
    onAddTokensClick: () -> Unit,
    shouldShowOrganizeButton: Boolean,
    onOrganizeTokensClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AddAndManageRow(
            iconRes = R.drawable.ic_plus_24,
            title = R.string.add_and_manage_sheet_manage_title,
            subtitle = R.string.add_and_manage_sheet_manage_subtitle,
            onClick = onAddTokensClick,
        )
        if (shouldShowOrganizeButton) {
            AddAndManageRow(
                iconRes = R.drawable.ic_filter_default_24,
                title = R.string.add_and_manage_sheet_organize_title,
                subtitle = R.string.add_and_manage_sheet_organize_subtitle,
                onClick = onOrganizeTokensClick,
            )
        }
    }
}

@Composable
private fun AddAndManageRow(
    iconRes: Int,
    title: Int,
    subtitle: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(TangemTheme.colors3.bg.tertiary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.icon.brand.copy(alpha = 0.1f)),
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = ImageVector.vectorResource(id = iconRes),
                tint = TangemTheme.colors3.icon.brand,
                contentDescription = null,
            )
        }
        SpacerW(12.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResourceSafe(id = title),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(id = subtitle),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        SpacerW(8.dp)
        Icon(
            imageVector = Icons.ic_chevron_right_24,
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AddAndManageBottomSheetContent_Preview() {
    TangemThemePreviewRedesign {
        AddAndManageContent(
            onAddTokensClick = {},
            shouldShowOrganizeButton = true,
            onOrganizeTokensClick = {},
        )
    }
}
// endregion