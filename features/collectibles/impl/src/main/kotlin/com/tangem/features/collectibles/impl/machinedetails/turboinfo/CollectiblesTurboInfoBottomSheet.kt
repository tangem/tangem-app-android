package com.tangem.features.collectibles.impl.machinedetails.turboinfo

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_28

@Composable
internal fun CollectiblesTurboInfoBottomSheet(onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = {
            TangemTopNavigation(
                title = TextReference.EMPTY,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
                onClose = onDismiss,
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TurboInfoMessage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
                )
                TangemFooter(
                    windowInsets = WindowInsets(0),
                    fadeEnabled = false,
                    blurBackground = false,
                    modifier = Modifier.padding(bottom = 24.dp),
                ) {
                    TangemButtonGroup {
                        TangemButton(
                            variant = TangemButton.Variant.Primary,
                            size = TangemButton.Size.X12,
                            text = resourceReference(R.string.common_got_it),
                            onClick = onDismiss,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun TurboInfoMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color = TangemTheme.colors3.bg.status.infoSubtle, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_info_28,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.info,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.collectibles_turbo_mode_bottomsheet_title),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResourceSafe(R.string.collectibles_turbo_mode_bottomsheet_message),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesTurboInfoMessage_Preview() {
    TangemThemePreviewRedesign {
        TurboInfoMessage(modifier = Modifier.padding(16.dp))
    }
}
// endregion