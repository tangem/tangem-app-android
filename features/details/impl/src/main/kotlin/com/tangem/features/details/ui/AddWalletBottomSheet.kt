package com.tangem.features.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.features.details.entity.AddWalletBS
import com.tangem.features.details.impl.R

@Composable
internal fun AddWalletBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<AddWalletBS>(
        config = config,
        titleText = resourceReference(R.string.user_wallet_add_wallet),
        titleAction = TopAppBarButtonUM.Close(onCloseClick = config.onDismissRequest),
        containerColor = TangemTheme.colors3.bg.secondary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: AddWalletBS) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        AddWalletOption(
            icon = Icons.ic_logo_tangem_20,
            title = resourceReference(R.string.user_wallet_add_hardware_title),
            subtitle = resourceReference(R.string.user_wallet_add_hardware_description),
            onClick = content.onAddHardwareWalletClick,
        )
        AddWalletOption(
            icon = Icons.ic_grid_plus_20,
            title = resourceReference(R.string.user_wallet_add_mobile_title),
            subtitle = resourceReference(R.string.user_wallet_add_mobile_description),
            onClick = content.onAddMobileWalletClick,
        )
    }
}

@Composable
private fun AddWalletOption(
    icon: ImageVector,
    title: TextReference,
    subtitle: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = {
            Icon(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = TangemTheme.colors3.bg.status.infoSubtle, shape = CircleShape)
                    .padding(10.dp),
                imageVector = icon,
                tint = TangemTheme.colors3.icon.brand,
                contentDescription = null,
            )
        },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = subtitle, role = TangemRowTextRole.Subtitle) },
        endSlot = {
            Icon(
                imageVector = Icons.ic_chevron_right_20,
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_AddWalletBottomSheet() {
    TangemThemePreview {
        AddWalletBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = AddWalletBS(
                    onAddHardwareWalletClick = {},
                    onAddMobileWalletClick = {},
                ),
            ),
        )
    }
}
// endregion Preview