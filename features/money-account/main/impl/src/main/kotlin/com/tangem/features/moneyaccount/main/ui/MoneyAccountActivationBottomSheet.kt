package com.tangem.features.moneyaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.appendWithStyledPlaceholder
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.moneyaccount.main.impl.R
import com.tangem.features.moneyaccount.main.ui.state.MoneyAccountActivationUM

@Composable
internal fun MoneyAccountActivationBottomSheet(state: MoneyAccountActivationUM) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(
                endButton = { TangemButton.Close(onClick = state.onDismiss) },
            )
        },
        content = {
            Header()
            Footer(
                isColdWallet = state.isColdWallet,
                isActivationInProgress = state.isActivationInProgress,
                onActivateClick = state.onActivateClick,
                onLaterClick = state.onDismiss,
                onTermsClick = state.onTermsClick,
            )
        },
    )
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        TangemAvatar(
            state = TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Money),
            size = TangemAvatar.Size.X72,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.money_account_main_name),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResourceSafe(R.string.money_account_main_status_dialog_not_started_description),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Footer(
    isColdWallet: Boolean,
    isActivationInProgress: Boolean,
    onActivateClick: () -> Unit,
    onLaterClick: () -> Unit,
    onTermsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_activate),
                iconEnd = if (isColdWallet) TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24) else null,
                isLoading = isActivationInProgress,
                onClick = onActivateClick,
            )
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_later),
                onClick = onLaterClick,
            )
        }
        TermsNotice(onTermsClick = onTermsClick)
    }
}

@Composable
private fun TermsNotice(onTermsClick: () -> Unit) {
    val linkStyle = SpanStyle(
        color = TangemTheme.colors3.text.primary,
        textDecoration = TextDecoration.None,
    )
    val termsTemplate = stringResourceSafe(R.string.money_account_main_status_dialog_not_started_terms_notice)
    val termsLink = stringResourceSafe(R.string.money_account_main_status_dialog_not_started_terms_link)
    val text = buildAnnotatedString {
        appendWithStyledPlaceholder(template = termsTemplate) {
            withLink(
                LinkAnnotation.Clickable(
                    tag = termsLink,
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { onTermsClick() },
                ),
            ) {
                append(termsLink)
            }
        }
    }
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountActivationBottomSheet_Preview() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        MoneyAccountActivationBottomSheet(
            state = MoneyAccountActivationUM(
                isColdWallet = true,
                isActivationInProgress = false,
                onActivateClick = {},
                onTermsClick = {},
                onDismiss = {},
            ),
        )
    }
}