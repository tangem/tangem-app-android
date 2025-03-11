package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tokendetails.impl.R

@Composable
internal fun StakingTemporaryUnavailableBlock(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResourceSafe(R.string.staking_native),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        Text(
            text = stringResourceSafe(R.string.staking_notification_network_error_text),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingTemporaryUnavailableBlock_Preview() {
    TangemThemePreview {
        StakingTemporaryUnavailableBlock(
            modifier = Modifier.padding(TangemTheme.dimens.spacing16),
        )
    }
}

// endregion