package com.tangem.feature.wallet.child.managetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R as ResR
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun AddAndManageBottomSheetContent(
    onAddTokensClick: () -> Unit,
    onOrganizeTokensClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val config = TangemBottomSheetConfig(
        isShown = true,
        onDismissRequest = onDismiss,
        content = AddAndManageBottomSheetConfigContent,
    )

    TangemModalBottomSheet<AddAndManageBottomSheetConfigContent>(
        config = config,
        containerColor = TangemTheme.colors.background.primary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(ResR.string.main_add_and_manage_tokens),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            AddAndManageContent(
                onAddTokensClick = onAddTokensClick,
                onOrganizeTokensClick = onOrganizeTokensClick,
            )
        },
    )
}

@Composable
private fun AddAndManageContent(onAddTokensClick: () -> Unit, onOrganizeTokensClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
    ) {
        AddAndManageRow(
            iconRes = R.drawable.ic_plus_24,
            title = ResR.string.add_and_manage_sheet_manage_title,
            subtitle = ResR.string.add_and_manage_sheet_manage_subtitle,
            onClick = onAddTokensClick,
            modifier = Modifier.roundedShapeItemDecoration(
                currentIndex = 0,
                lastIndex = 1,
                addDefaultPadding = false,
                backgroundColor = TangemTheme.colors.background.action,
            ),
        )
        AddAndManageRow(
            iconRes = R.drawable.ic_filter_default_24,
            title = ResR.string.add_and_manage_sheet_organize_title,
            subtitle = ResR.string.add_and_manage_sheet_organize_subtitle,
            onClick = onOrganizeTokensClick,
            modifier = Modifier.roundedShapeItemDecoration(
                currentIndex = 1,
                lastIndex = 1,
                addDefaultPadding = false,
                backgroundColor = TangemTheme.colors.background.action,
            ),
        )
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
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors.icon.accent.copy(alpha = 0.1f)),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = rememberVectorPainter(ImageVector.vectorResource(id = iconRes)),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResourceSafe(id = title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = stringResourceSafe(id = subtitle),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

private object AddAndManageBottomSheetConfigContent : TangemBottomSheetConfigContent

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AddAndManageBottomSheetContent_Preview() {
    TangemThemePreview {
        AddAndManageContent(
            onAddTokensClick = {},
            onOrganizeTokensClick = {},
        )
    }
}
// endregion