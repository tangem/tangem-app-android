package com.tangem.features.polymarket.impl.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.R as CoreR
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM

@Composable
internal fun PolymarketOnboardingScreen(
    state: PolymarketOnboardingUM,
    onRegionRestrictionsDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            PolymarketOnboardingUM.Loading,
            PolymarketOnboardingUM.RegionBlocked,
            -> TangemLoader(color = TangemTheme.colors3.icon.primary, size = TangemLoaderSize.X32)
            PolymarketOnboardingUM.Welcome -> WelcomePlaceholder()
            is PolymarketOnboardingUM.Failed -> FailedState(onRetryClick = state.onRetryClick)
        }
    }

    RegionRestrictionsBottomSheet(
        isShown = state is PolymarketOnboardingUM.RegionBlocked,
        onDismiss = onRegionRestrictionsDismiss,
    )
}

@Composable
private fun WelcomePlaceholder(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 24.dp),
        text = "Trade with predictions",
        style = TangemTheme.typography3.heading.medium,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FailedState(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_something_went_wrong),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            size = TangemButton.Size.X10,
            variant = TangemButton.Variant.Primary,
            text = resourceReference(R.string.common_retry),
            onClick = onRetryClick,
        )
    }
}

@Composable
private fun RegionRestrictionsBottomSheet(isShown: Boolean, onDismiss: () -> Unit) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = CoreR.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Icon(
                    modifier = Modifier.size(80.dp),
                    imageVector = Icons.ic_error_28,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.status.warning,
                )
                Text(
                    text = stringResourceSafe(R.string.prediction_region_restrictions_title),
                    style = TangemTheme.typography3.heading.small,
                    color = TangemTheme.colors3.text.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResourceSafe(R.string.prediction_region_restrictions_subtitle),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                )
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    size = TangemButton.Size.X10,
                    variant = TangemButton.Variant.Primary,
                    text = resourceReference(R.string.common_close),
                    onClick = onDismiss,
                )
            }
        },
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenRegionBlockedPreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(
            state = PolymarketOnboardingUM.RegionBlocked,
            onRegionRestrictionsDismiss = {},
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenFailedPreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(
            state = PolymarketOnboardingUM.Failed(onRetryClick = {}),
            onRegionRestrictionsDismiss = {},
        )
    }
}