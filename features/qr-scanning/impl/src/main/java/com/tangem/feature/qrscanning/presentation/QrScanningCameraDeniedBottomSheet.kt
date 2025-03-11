package com.tangem.feature.qrscanning.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.qrscanning.impl.R

@Composable
fun CameraDeniedBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        title = {
            CameraDeniedBottomSheetHeader()
        },
    ) { content: CameraDeniedBottomSheetConfig ->
        CameraDeniedBottomSheet(content = content)
    }
}

@Composable
private fun CameraDeniedBottomSheet(content: CameraDeniedBottomSheetConfig) {
    Column {
        SimpleSettingsRow(
            title = stringResourceSafe(id = R.string.qr_scanner_camera_denied_settings_button),
            icon = R.drawable.ic_settings_24,
            onItemsClick = content.onSettingsClick,
        )
        SimpleSettingsRow(
            title = stringResourceSafe(id = R.string.qr_scanner_camera_denied_gallery_button),
            icon = R.drawable.ic_gallery_24,
            onItemsClick = content.onGalleryClick,
        )
        SimpleSettingsRow(
            title = stringResourceSafe(id = R.string.common_close),
            icon = R.drawable.ic_close_24,
            onItemsClick = content.onCancelClick,
            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun CameraDeniedBottomSheetHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                vertical = TangemTheme.dimens.spacing16,
                horizontal = TangemTheme.dimens.spacing20,
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.qr_scanner_camera_denied_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = stringResourceSafe(id = R.string.qr_scanner_camera_denied_text),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CameraDeniedBottomSheet() {
    TangemThemePreview {
        CameraDeniedBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = CameraDeniedBottomSheetConfig(
                    onSettingsClick = {},
                    onCancelClick = {},
                    onGalleryClick = {},
                ),
            ),
        )
    }
}
// endregion Preview