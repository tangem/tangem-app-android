package com.tangem.features.hotwallet.createmobilewallet.importoptions.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_download_24
import com.tangem.features.hotwallet.createmobilewallet.importoptions.entity.ImportOptionsBottomSheetUM
import com.tangem.features.hotwallet.impl.R

@Composable
internal fun ImportOptionsBottomSheet(state: ImportOptionsBottomSheetUM) {
    TangemModalBottomSheet<ImportOptionsBottomSheetUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemButton.Close(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                onClick = state.onDismiss,
            )
        },
        content = { contentState ->
            when (val content = contentState.content) {
                is ImportOptionsBottomSheetUM.Content.Options -> OptionsContent(content)
                is ImportOptionsBottomSheetUM.Content.Error -> ErrorContent(content)
            }
        },
    )
}

@Composable
private fun OptionsContent(content: ImportOptionsBottomSheetUM.Content.Options, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        HeaderBlock(
            title = stringResourceSafe(R.string.hw_import_existing_wallet),
            body = stringResourceSafe(R.string.hw_import_existing_wallet_description),
        ) {
            StatusIconCircle(
                painter = rememberVectorPainter(Icons.ic_arrow_download_24),
                tint = TangemTheme.colors3.icon.status.info,
                background = TangemTheme.colors3.bg.status.infoSubtle,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(R.string.hw_cloud_backup_restore_use_recovery_phrase),
                onClick = content.onRecoveryPhraseClick,
                enabled = !content.isCloudLoading,
            )
            SecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(
                    R.string.hw_import_restore_cloud_backup,
                    stringResourceSafe(R.string.hw_cloud_backup_service_name),
                ),
                onClick = content.onCloudBackupClick,
                showProgress = content.isCloudLoading,
                enabled = !content.isCloudLoading,
            )
        }
    }
}

@Composable
private fun ErrorContent(content: ImportOptionsBottomSheetUM.Content.Error, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        HeaderBlock(
            title = content.title.resolveReference(),
            body = content.body.resolveReference(),
        ) {
            StatusIconCircle(
                painter = painterResource(id = R.drawable.ic_alert_triangle_20),
                tint = if (content.isWarning) {
                    TangemTheme.colors3.icon.status.warning
                } else {
                    TangemTheme.colors3.icon.status.error
                },
                background = if (content.isWarning) {
                    TangemTheme.colors3.bg.status.warningSubtle
                } else {
                    TangemTheme.colors3.bg.status.errorSubtle
                },
            )
        }
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .padding(all = 16.dp),
            text = stringResourceSafe(R.string.common_got_it),
            onClick = content.onGotItClick,
        )
    }
}

@Composable
private fun HeaderBlock(title: String, body: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = title,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = body,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusIconCircle(painter: Painter, tint: Color, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportOptionsBottomSheet_Options_Preview() {
    TangemThemePreviewRedesign {
        ImportOptionsBottomSheet(
            state = ImportOptionsBottomSheetUM(
                content = ImportOptionsBottomSheetUM.Content.Options(
                    isCloudLoading = false,
                    onRecoveryPhraseClick = {},
                    onCloudBackupClick = {},
                ),
                onDismiss = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportOptionsBottomSheet_Error_Preview() {
    TangemThemePreviewRedesign {
        ImportOptionsBottomSheet(
            state = ImportOptionsBottomSheetUM(
                content = ImportOptionsBottomSheetUM.Content.Error(
                    title = resourceReference(R.string.hw_cloud_backup_error_title),
                    body = resourceReference(R.string.hw_cloud_backup_restore_error_with_recovery),
                    isWarning = false,
                    onGotItClick = {},
                ),
                onDismiss = {},
            ),
        )
    }
}