package com.tangem.features.tangempay.addfunds.va.bank

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.common.TangemPayErrorAction
import com.tangem.features.tangempay.common.TangemPayErrorContent
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.persistentListOf

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
    TangemPayErrorContent(
        title = resourceReference(R.string.tangempay_va_banking_details_error_title),
        subtitle = resourceReference(R.string.tangempay_va_banking_details_error_description),
        actions = persistentListOf(
            TangemPayErrorAction(
                text = resourceReference(R.string.common_contact_support),
                variant = TangemButton.Variant.Secondary,
                onClick = state.onContactSupportClick,
                isEnabled = !state.isRetryLoading,
            ),
            TangemPayErrorAction(
                text = resourceReference(R.string.common_retry),
                variant = TangemButton.Variant.Primary,
                onClick = state.onRetryClick,
                isLoading = state.isRetryLoading,
                isEnabled = !state.isRetryLoading,
            ),
        ),
        modifier = modifier,
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