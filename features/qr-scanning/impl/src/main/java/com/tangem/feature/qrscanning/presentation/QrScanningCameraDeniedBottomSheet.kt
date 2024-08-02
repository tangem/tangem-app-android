package com.tangem.feature.qrscanning.presentation

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
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
    val context = LocalContext.current
    Column {
        SimpleSettingsRow(
            title = stringResource(id = R.string.qr_scanner_camera_denied_settings_button),
            icon = R.drawable.ic_settings_24,
            onItemsClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                ContextCompat.startActivity(context, intent, null)
            },
        )
        SimpleSettingsRow(
            title = stringResource(id = R.string.qr_scanner_camera_denied_gallery_button),
            icon = R.drawable.ic_gallery_24,
            onItemsClick = content.onGalleryClick,
        )
        SimpleSettingsRow(
            title = stringResource(id = R.string.common_close),
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
            text = stringResource(id = R.string.qr_scanner_camera_denied_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = stringResource(id = R.string.qr_scanner_camera_denied_text),
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
                isShow = true,
                onDismissRequest = {},
                content = CameraDeniedBottomSheetConfig(
                    onCancelClick = {},
                    onGalleryClick = {},
                ),
            ),
        )
    }
}
// endregion Preview