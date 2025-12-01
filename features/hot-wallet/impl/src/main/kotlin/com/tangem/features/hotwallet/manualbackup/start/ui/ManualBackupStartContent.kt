package com.tangem.features.hotwallet.manualbackup.start.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.feature.FeatureBlock
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.manualbackup.start.entity.ManualBackupStartUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualBackupStartContent(state: ManualBackupStartUM, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .padding(
                start = 16.dp,
                top = 24.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                text = stringResourceSafe(R.string.backup_info_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                text = stringResourceSafe(
                    R.string.backup_info_description,
                    state.seepPhraseLength.toString(),
                ),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                title = stringResourceSafe(R.string.backup_info_save_title),
                description = stringResourceSafe(
                    R.string.backup_info_save_description,
                    state.seepPhraseLength.toString(),
                ),
                iconRes = R.drawable.ic_lock_24,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                title = stringResourceSafe(R.string.backup_info_keep_title),
                description = stringResourceSafe(R.string.backup_info_keep_description),
                iconRes = R.drawable.ic_settings_24,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        PrimaryButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = 16.dp),
            text = stringResourceSafe(R.string.common_continue),
            showProgress = false,
            enabled = true,
            onClick = state.onContinueClick,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewManualBackupStartContent() {
    TangemThemePreview {
        ManualBackupStartContent(
            state = ManualBackupStartUM(
                onContinueClick = {},
            ),
        )
    }
}