package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayVaBankingDetailsErrorUM

@Composable
internal fun TangemPayVaBankingDetailsErrorBottomSheet(state: TangemPayVaBankingDetailsErrorUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                title = null,
                endContent = { TangemButton.Close(onClick = state.onDismiss) },
            )
        },
        content = { _ -> Content(state) },
    )
}

@Composable
private fun Content(state: TangemPayVaBankingDetailsErrorUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WarningIcon(modifier = Modifier.padding(top = TangemTheme.dimens2.x4))
        TitleText(
            text = resourceReference(R.string.tangempay_va_banking_details_error_title),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x8),
        )
        SubtitleText(
            text = resourceReference(R.string.tangempay_va_banking_details_error_description),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x2),
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens2.x8),
            text = resourceReference(R.string.common_contact_support),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            isEnabled = !state.isRetryLoading,
            onClick = state.onContactSupportClick,
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens2.x2),
            text = resourceReference(R.string.common_retry),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            isLoading = state.isRetryLoading,
            isEnabled = !state.isRetryLoading,
            onClick = state.onRetryClick,
        )
    }
}

@Composable
private fun WarningIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x20)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.status.warningSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x7),
            imageVector = Icons.ic_error_28,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.warning,
        )
    }
}

@Composable
private fun TitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SubtitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayVaBankingDetailsErrorPreview(
    @PreviewParameter(VaBankingDetailsErrorPreviewProvider::class) state: TangemPayVaBankingDetailsErrorUM,
) {
    TangemThemePreviewRedesign {
        Content(
            state = state,
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

private class VaBankingDetailsErrorPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayVaBankingDetailsErrorUM>(
        collection = listOf(
            TangemPayVaBankingDetailsErrorUM(
                isRetryLoading = false,
                onRetryClick = {},
                onContactSupportClick = {},
                onDismiss = {},
            ),
            TangemPayVaBankingDetailsErrorUM(
                isRetryLoading = true,
                onRetryClick = {},
                onContactSupportClick = {},
                onDismiss = {},
            ),
        ),
    )