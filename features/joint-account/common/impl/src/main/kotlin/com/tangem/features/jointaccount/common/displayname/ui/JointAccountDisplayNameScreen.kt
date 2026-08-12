package com.tangem.features.jointaccount.common.displayname.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM

@Composable
internal fun JointAccountDisplayNameScreen(
    state: JointAccountDisplayNameUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary),
    ) {
        var topBarHeightPx by remember { mutableIntStateOf(0) }
        var footerHeightPx by remember { mutableIntStateOf(0) }
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val footerHeight = with(density) { footerHeightPx.toDp() }
        val bottomNavHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .background(color = TangemTheme.colors3.bg.primary)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = topBarHeight + 12.dp, bottom = footerHeight + 12.dp + bottomNavHeight),
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

        Footer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .onSizeChanged { footerHeightPx = it.height },
            state = state,
        )

        TangemTopNavigation(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeightPx = it.height },
            startButton = { TangemButton.Back(onClick = state.onBackClick) },
            endButton = { TangemButton.Close(onClick = onCloseClick) },
        )
    }
}

@Composable
private fun NameField(state: JointAccountDisplayNameUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .padding(vertical = 2.dp),
            value = state.name,
            textStyle = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onValueChange = state.onNameChange,
        )

        SpacerH(12.dp)

        Underline(isError = state.isError, isFocused = isFocused)

        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = stringResourceSafe(R.string.joint_account_name_hint),
            style = TangemTheme.typography3.caption.medium,
            color = if (state.isError) {
                TangemTheme.colors3.text.status.error
            } else {
                TangemTheme.colors3.text.secondary
            },
        )
    }
}

@Composable
private fun Underline(isError: Boolean, isFocused: Boolean, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = when {
            isError -> TangemTheme.colors3.border.status.error
            isFocused -> TangemTheme.colors3.border.status.info
            else -> TangemTheme.colors3.border.tertiary
        },
    )
}

@Composable
private fun Footer(state: JointAccountDisplayNameUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            modifier = Modifier.matchParentSize(),
        )

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = state.buttonText,
            iconEnd = state.buttonIconRes?.let { iconRes ->
                TangemIconUM.Icon(imageVector = ImageVector.vectorResource(iconRes))
            },
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
        createPreviewState(name = "", isError = false, isButtonEnabled = false),
        createPreviewState(name = "Ivan Zolo", isError = false, isButtonEnabled = true),
        // A forbidden character highlights the field
        createPreviewState(name = "Ivan Zolo!", isError = true, isButtonEnabled = false),
        // The signature session is in progress
        createPreviewState(name = "Ivan Zolo", isError = false, isButtonEnabled = true, isButtonLoading = true),
    ),
)

private fun createPreviewState(
    name: String,
    isError: Boolean,
    isButtonEnabled: Boolean,
    isButtonLoading: Boolean = false,
): JointAccountDisplayNameUM {
    return JointAccountDisplayNameUM(
        name = name,
        isError = isError,
        buttonText = resourceReference(R.string.common_create_account),
        buttonIconRes = R.drawable.ic_tangem_24,
        isButtonEnabled = isButtonEnabled,
        isButtonLoading = isButtonLoading,
        onNameChange = {},
        onContinueClick = {},
        onBackClick = {},
    )
}