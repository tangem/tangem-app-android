package com.tangem.features.tangempay.verified

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.pay.TangemPayProduct

@Composable
internal fun TangemPayVerifiedScreen(state: TangemPayVerifiedUM, animated: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        when (state) {
            is TangemPayVerifiedUM.Loading -> LoadingContent()
            is TangemPayVerifiedUM.Content -> StatusContent(state = state, animated = animated)
        }
        TangemTopNavigation(
            modifier = Modifier.align(Alignment.TopCenter),
            endButton = { TangemButton.Close(onClick = state.onCloseClick) },
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(size = TangemLoaderSize.X32)
    }
}

@Composable
private fun StatusContent(state: TangemPayVerifiedUM.Content, animated: Boolean) {
    TangemStatusScreen(
        variant = TangemStatusScreen.Variant.Success,
        title = resourceReference(R.string.tangempay_verified_title),
        description = state.description,
        animated = animated,
        asset = {
            TangemStatusIcon(
                kind = TangemStatusIcon.Kind.Checkmark,
                modifier = Modifier.size(40.dp),
                animated = animated,
            )
        },
        details = {
            ProductRow(product = state.moneyAccount, divider = true)
            ProductRow(product = state.visaCard, divider = false)
        },
        footer = {
            TangemButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                state.secondaryButton?.let { button ->
                    TangemButton(
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X12,
                        text = button.text,
                        onClick = button.onClick,
                    )
                }
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = state.primaryButton.text,
                    onClick = state.primaryButton.onClick,
                )
            }
        },
    )
}

@Composable
private fun ProductRow(product: TangemPayVerifiedUM.ProductUM, divider: Boolean) {
    TangemRow(
        divider = divider,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            Text(
                text = product.title.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = if (product.isAvailable) {
                    TangemTheme.colors3.text.primary
                } else {
                    TangemTheme.colors3.text.secondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitleSlot = {
            TangemRowText(text = product.subtitle, role = TangemRowTextRole.Subtitle)
        },
        endSlot = {
            TangemBadge(
                text = product.badge.text,
                variant = product.badge.variant,
                status = product.badge.status,
                size = product.badge.size,
            )
        },
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayVerifiedScreenPreview(
    @PreviewParameter(TangemPayVerifiedPreviewProvider::class) state: TangemPayVerifiedUM,
) {
    TangemThemePreviewRedesign {
        TangemPayVerifiedScreen(state = state, animated = false)
    }
}

private class TangemPayVerifiedPreviewProvider : CollectionPreviewParameterProvider<TangemPayVerifiedUM>(
    collection = listOf(TangemPayVerifiedUM.Loading(onCloseClick = {})) + listOf(
        setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT),
        setOf(TangemPayProduct.PAYMENT_ACCOUNT),
        setOf(TangemPayProduct.BANKING_ACCOUNT),
        emptySet(),
    ).map(
        TangemPayVerifiedUMFactory(
            onChoosePlanClick = {},
            onOpenMoneyAccountClick = {},
            onNotifyClick = {},
            onCloseClick = {},
        )::create,
    ),
)