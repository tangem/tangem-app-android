package com.tangem.features.tangempay.eligibility.waitlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckbox
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_24_filled
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TangemPayWaitlistBottomSheet(state: TangemPayWaitlistUM) {
    TangemModal<TangemPayWaitlistUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        scrollableContent = true,
        title = {
            TangemModal.TopNavigation(
                endButton = { TangemButton.Close(onClick = state.onDismiss) },
            )
        },
        content = { content -> Content(state = content) },
    )
}

@Composable
private fun Content(state: TangemPayWaitlistUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.imePadding()) {
        Header()

        EmailField(state = state)

        MarketingConsent(state = state)

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_save),
            isEnabled = state.isSaveEnabled,
            isLoading = state.isSaveInProgress,
            onClick = state.onSaveClick,
        )
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_waitlist_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = stringResourceSafe(R.string.tangempay_waitlist_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun EmailField(state: TangemPayWaitlistUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_email),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        SpacerH4()
        EmailTextField(
            state = state,
            onFocusChange = { isFocused = it.isFocused },
            focusRequester = focusRequester,
        )
        SpacerH(14.dp)
        HorizontalDivider(
            thickness = 1.dp,
            color = when {
                state.emailError != null -> TangemTheme.colors3.border.status.error
                isFocused -> TangemTheme.colors3.border.brand
                else -> TangemTheme.colors3.border.tertiary
            },
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = state.emailError?.resolveReference().orEmpty(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.status.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmailTextField(
    state: TangemPayWaitlistUM,
    onFocusChange: (FocusState) -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SimpleTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged(onFocusChange),
            value = state.email,
            placeholder = resourceReference(R.string.tangempay_waitlist_email_placeholder),
            textStyle = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            placeholderColor = TangemTheme.colors3.text.tertiary,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            onValueChange = state.onEmailChange,
        )
        if (state.email.isNotEmpty()) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickableSingle(onClick = state.onEmailClearClick),
                imageVector = Icons.ic_cross_circle_24_filled,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.tertiary,
            )
        }
    }
}

@Composable
private fun MarketingConsent(state: TangemPayWaitlistUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TangemCheckbox(
            state = if (state.isMarketingConsentGiven) ToggleableState.On else ToggleableState.Off,
            onClick = state.onMarketingConsentClick,
        )
        MarketingConsentText(onPrivacyPolicyClick = state.onPrivacyPolicyClick)
    }
}

@Composable
private fun MarketingConsentText(onPrivacyPolicyClick: () -> Unit, modifier: Modifier = Modifier) {
    val privacyPolicyTitle = stringResourceSafe(R.string.common_privacy_policy)
    val fullText = stringResourceSafe(R.string.tangempay_waitlist_marketing_consent, privacyPolicyTitle)
    val linkIndex = fullText.indexOf(privacyPolicyTitle)
    val linkStyle = SpanStyle(
        color = TangemTheme.colors3.text.primary,
        textDecoration = TextDecoration.None,
    )
    val text = buildAnnotatedString {
        if (linkIndex < 0) {
            append(fullText)
            return@buildAnnotatedString
        }

        append(fullText.substring(0, linkIndex))
        withLink(
            LinkAnnotation.Clickable(
                tag = privacyPolicyTitle,
                linkInteractionListener = { onPrivacyPolicyClick() },
            ),
        ) {
            withStyle(linkStyle) { append(privacyPolicyTitle) }
        }
        append(fullText.substring(linkIndex + privacyPolicyTitle.length))
    }

    Text(
        modifier = modifier,
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Empty() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        TangemPayWaitlistBottomSheet(state = previewState(email = "", isSaveEnabled = false))
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_EmailError() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        TangemPayWaitlistBottomSheet(
            state = previewState(
                email = "satoshi.nakamoto@tangem",
                isSaveEnabled = true,
                emailError = resourceReference(R.string.common_error_email_incorrect_format),
            ),
        )
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Filled() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        TangemPayWaitlistBottomSheet(
            state = previewState(
                email = "satoshi.nakamoto@tangem.com",
                isSaveEnabled = true,
                isMarketingConsentGiven = true,
            ),
        )
    }
}

private fun previewState(
    email: String,
    isSaveEnabled: Boolean,
    isMarketingConsentGiven: Boolean = false,
    isSaveInProgress: Boolean = false,
    emailError: TextReference? = null,
) = TangemPayWaitlistUM(
    email = email,
    emailError = emailError,
    isMarketingConsentGiven = isMarketingConsentGiven,
    isSaveEnabled = isSaveEnabled,
    isSaveInProgress = isSaveInProgress,
    onEmailChange = {},
    onEmailClearClick = {},
    onMarketingConsentClick = {},
    onPrivacyPolicyClick = {},
    onSaveClick = {},
    onDismiss = {},
)