package com.tangem.features.tangempay.success

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TangemPayOnboardingSuccessScreen(
    onCloseClick: () -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        variant = TangemStatusScreen.Variant.Info,
        title = resourceReference(R.string.tangempay_onboarding_success_title),
        description = resourceReference(R.string.tangempay_onboarding_success_description),
        animated = animated,
        modifier = modifier,
        asset = {
            TangemStatusIcon(
                kind = TangemStatusIcon.Kind.Checkmark,
                tint = TangemTheme.colors3.icon.status.info,
                modifier = Modifier.size(40.dp),
                animated = animated,
            )
        },
        footer = {
            TangemButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_close),
                    onClick = onCloseClick,
                )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayOnboardingSuccessScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayOnboardingSuccessScreen(onCloseClick = {}, animated = false)
    }
}