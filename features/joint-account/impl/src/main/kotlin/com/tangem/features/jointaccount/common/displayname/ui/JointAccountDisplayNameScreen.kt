package com.tangem.features.jointaccount.common.displayname.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM

@Composable
internal fun JointAccountDisplayNameScreen(
    state: JointAccountDisplayNameUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var footerHeightPx by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                startButton = { TangemButton.Back(onClick = state.onBackClick) },
                endButton = { TangemButton.Close(onClick = onCloseClick) },
            )
        },
        overlay = {
            Footer(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .onSizeChanged { size -> footerHeightPx = size.height },
            )
        },
    ) { contentPadding ->
        val footerHeight = with(LocalDensity.current) { footerHeightPx.toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = maxOf(contentPadding.calculateBottomPadding(), footerHeight) + 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResourceSafe(R.string.joint_account_name_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )

            Text(
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                text = stringResourceSafe(R.string.joint_account_name_subtitle),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )

            NameField(state = state, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun NameField(state: JointAccountDisplayNameUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // The card session takes over the screen. Hiding the keyboard alone is not enough: a still-focused field pulls
    // it back the moment `readOnly` is lifted, so the focus goes too
    LaunchedEffect(state.isButtonLoading) {
        if (state.isButtonLoading) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResourceSafe(R.string.joint_account_name_display_name),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )

        SpacerH(2.dp)

        SimpleTextField(
            modifier = Modifier
                .testTag(JointAccountCreationTestTags.DISPLAY_NAME_FIELD)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .padding(vertical = 2.dp),
            value = state.name,
            textStyle = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            singleLine = true,
            readOnly = state.isButtonLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onValueChange = state.onNameChange,
        )

        SpacerH(12.dp)

        Underline(isFocused = isFocused)

        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = stringResourceSafe(R.string.joint_account_name_hint),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun Underline(isFocused: Boolean, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = if (isFocused) TangemTheme.colors3.border.status.info else TangemTheme.colors3.border.tertiary,
    )
}

@Composable
private fun Footer(state: JointAccountDisplayNameUM, modifier: Modifier = Modifier) {
    TangemFooter(modifier = modifier) {
        TangemButton(
            modifier = Modifier
                .testTag(JointAccountCreationTestTags.DISPLAY_NAME_CREATE_BUTTON)
                .fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = state.buttonText,
            iconEnd = state.buttonIcon,
            isEnabled = state.isButtonEnabled,
            isLoading = state.isButtonLoading,
            onClick = state.onContinueClick,
        )
    }
}

@Preview(showBackground = true, heightDp = 874)
@Preview(showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountDisplayNameScreen(
    @PreviewParameter(JointAccountDisplayNameStateProvider::class) state: JointAccountDisplayNameUM,
) {
    TangemThemePreviewRedesign {
        JointAccountDisplayNameScreen(state = state, onCloseClick = {})
    }
}

private class JointAccountDisplayNameStateProvider : CollectionPreviewParameterProvider<JointAccountDisplayNameUM>(
    collection = listOf(
        // Empty input: the button is disabled
        createPreviewState(name = "", isButtonEnabled = false),
        createPreviewState(name = "Ivan Zolo", isButtonEnabled = true),
        // The signature session is in progress
        createPreviewState(name = "Ivan Zolo", isButtonEnabled = true, isButtonLoading = true),
    ),
)

private fun createPreviewState(
    name: String,
    isButtonEnabled: Boolean,
    isButtonLoading: Boolean = false,
): JointAccountDisplayNameUM {
    return JointAccountDisplayNameUM(
        name = name,
        buttonText = resourceReference(R.string.common_create_account),
        buttonIcon = TangemIconUM.Icon(iconRes = R.drawable.ic_tangem_24),
        isButtonEnabled = isButtonEnabled,
        isButtonLoading = isButtonLoading,
        onNameChange = {},
        onContinueClick = {},
        onBackClick = {},
    )
}