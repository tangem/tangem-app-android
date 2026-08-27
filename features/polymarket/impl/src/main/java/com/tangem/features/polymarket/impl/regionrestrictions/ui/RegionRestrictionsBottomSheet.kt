package com.tangem.features.polymarket.impl.regionrestrictions.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.core.ui.R as CoreR

/**
 * Tells the user that predictions are unavailable in their region.
 *
 * Deliberately knows nothing about onboarding: the same sheet is the right answer wherever a restricted
 * region has to be stated, so callers own both when it is shown and what dismissing it means.
 */
@Composable
internal fun RegionRestrictionsBottomSheet(isShown: Boolean, onDismiss: () -> Unit) {
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
private fun RegionRestrictionsBottomSheetPreview() {
    TangemThemePreviewRedesign {
        RegionRestrictionsBottomSheet(isShown = true, onDismiss = {})
    }
}