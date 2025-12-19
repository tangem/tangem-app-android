package com.tangem.tap.features.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.icons.HighlightedIcon
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.wallet.R

@Composable
internal fun RootDetectedWarningContent(modifier: Modifier = Modifier, onContinueClick: () -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            InfoBlock(
                modifier = Modifier.padding(top = 48.dp, bottom = 24.dp),
            )
        }

        PrimaryButton(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.common_understand_continue),
            onClick = onContinueClick,
        )
    }
}

@Composable
private fun InfoBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HighlightedIcon(
            icon = R.drawable.ic_alert_circle_24,
            iconTint = TangemTheme.colors.icon.warning,
        )

        SpacerH(20.dp)

        Text(
            text = stringResourceSafe(R.string.root_detected_warning_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        SpacerH(12.dp)

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResourceSafe(R.string.root_detected_warning_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        RootDetectedWarningContent()
    }
}