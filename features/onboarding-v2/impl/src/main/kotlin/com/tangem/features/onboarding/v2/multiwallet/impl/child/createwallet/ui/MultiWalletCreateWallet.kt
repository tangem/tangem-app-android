package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.extensions.appendWithStyledPlaceholder
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.StoriesScreenTestTags
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.state.MultiWalletCreateWalletUM

@Suppress("LongMethod")
@Composable
internal fun MultiWalletCreateWallet(state: MultiWalletCreateWalletUM, modifier: Modifier = Modifier) {
    MultiWalletCreateWalletDialog(state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag(StoriesScreenTestTags.TITLE),
            )

            Text(
                text = state.bodyText.resolveReference(),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .testTag(StoriesScreenTestTags.TEXT),
            )
        }

        val termsTemplate = stringResourceSafe(CoreUiR.string.onboarding_create_wallet_term_of_conditions_text)
        val termsLinkText = stringResourceSafe(CoreUiR.string.disclaimer_title)
        val termsLinkColor = TangemTheme.colors.text.accent
        Text(
            text = buildAnnotatedString {
                appendWithStyledPlaceholder(template = termsTemplate) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "tos_link",
                            styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.None)),
                        ) { state.onTermsOfUseClick() },
                    ) {
                        appendColored(text = termsLinkText, color = termsLinkColor)
                    }
                }
            },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            text = stringResourceSafe(R.string.onboarding_create_wallet_button_create_wallet),
            onClick = state.onCreateWalletClick,
        )

        if (state.showOtherOptionsButton) {
            SecondaryButton(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.onboarding_create_wallet_options_button_options),
                onClick = state.onOtherOptionsClick,
            )
        }
    }
}

@Composable
private fun MultiWalletCreateWalletDialog(state: MultiWalletCreateWalletUM) {
    if (state.dialog != null) {
        BasicDialog(
            title = state.dialog.title.resolveReference(),
            message = state.dialog.message.resolveReference(),
            confirmButton = DialogButtonUM(
                title = state.dialog.confirmButtonText.resolveReference(),
                onClick = state.dialog.onConfirmClick,
            ),
            dismissButton = DialogButtonUM(
                title = state.dialog.dismissButtonText.resolveReference(),
                isWarning = state.dialog.dismissWarningColor,
                onClick = state.dialog.onDismissButtonClick,
            ),
            onDismissDialog = state.dialog.onDismiss,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletCreateWallet(
            state = MultiWalletCreateWalletUM(
                title = stringReference("Title"),
                bodyText = stringReference("Body body body"),
                onCreateWalletClick = {},
                showOtherOptionsButton = true,
                onOtherOptionsClick = {},
                onTermsOfUseClick = {},
                dialog = null,
            ),
        )
    }
}