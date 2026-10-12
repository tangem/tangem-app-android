package com.tangem.features.tangempay.withdraw.bank.form.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.common.SelectorInput
import com.tangem.features.tangempay.common.TextInput
import com.tangem.features.tangempay.common.TextInputState
import com.tangem.features.tangempay.common.revealAboveFooter
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.FieldUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.SuggestionUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayBankWithdrawFormScreen(state: TangemPayBankWithdrawFormUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = state.header.title,
                subtitle = state.header.subtitle,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
                onClose = state.header.onCloseClick,
            )
        },
        overlay = {
            FormFooter(
                isContinueEnabled = state.isContinueEnabled,
                onContinueClick = state.onContinueClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(footerInsets())
                .verticalScroll(rememberScrollState())
                .padding(top = contentPadding.calculateTopPadding())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = FooterHeight),
            verticalArrangement = Arrangement.spacedBy(FieldSpacing),
        ) {
            val accountNumberIndex = state.fields.indexOfFirst { it.field == BankWithdrawFormField.AccountNumber }
            if (accountNumberIndex == -1) {
                state.fields.fastForEach { field -> FormField(state = field) }
            } else {
                state.fields.subList(fromIndex = 0, toIndex = accountNumberIndex + 1)
                    .fastForEach { field -> FormField(state = field) }
                SuggestionsOrFields(
                    suggestions = state.suggestions,
                    fields = state.fields.subList(fromIndex = accountNumberIndex + 1, toIndex = state.fields.size),
                )
            }
        }
    }
}

@Composable
private fun SuggestionsOrFields(
    suggestions: ImmutableList<SuggestionUM>,
    fields: List<FieldUM>,
    modifier: Modifier = Modifier,
) {
    // Keyed on emptiness so typing only updates the list in place, while the outgoing list keeps its items to fade.
    AnimatedContent(
        targetState = suggestions,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(FADE_DURATION_MILLIS)) togetherWith fadeOut(tween(FADE_DURATION_MILLIS))
        },
        contentKey = { it.isNotEmpty() },
    ) { shownSuggestions ->
        if (shownSuggestions.isNotEmpty()) {
            BankAccountSuggestions(suggestions = shownSuggestions)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing)) {
                fields.fastForEach { field -> FormField(state = field) }
            }
        }
    }
}

@Composable
private fun FormField(state: FieldUM) {
    val modifier = Modifier
        .fillMaxWidth()
        .revealAboveFooter(footerHeight = FooterHeight)
    when (state) {
        is FieldUM.Text -> TextField(state = state, modifier = modifier)
        is FieldUM.Selector -> SelectorField(state = state, modifier = modifier)
    }
}

@Composable
private fun TextField(state: FieldUM.Text, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    TextInput(
        label = state.field.label,
        value = state.value,
        onValueChange = state.onValueChange,
        modifier = modifier,
        state = inputState(error = state.error),
        placeholder = state.placeholder,
        keyboardOptions = state.keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        contentType = state.contentType,
        onFocusChange = state.onFocusChange,
    )
}

@Composable
private fun SelectorField(state: FieldUM.Selector, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    SelectorInput(
        label = state.field.label,
        value = state.value,
        placeholder = state.placeholder,
        state = inputState(error = state.error),
        onClick = {
            focusManager.clearFocus()
            state.onClick()
        },
        modifier = modifier,
    )
}

private fun inputState(error: TextReference?): TextInputState =
    if (error != null) TextInputState.Error(text = error) else TextInputState.Default

@Composable
private fun FormFooter(isContinueEnabled: Boolean, onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    TangemFooter(
        modifier = modifier.windowInsetsPadding(WindowInsets.ime),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_continue),
            isEnabled = isContinueEnabled,
            onClick = {
                focusManager.clearFocus()
                onContinueClick()
            },
        )
    }
}

@Composable
private fun footerInsets(): WindowInsets = WindowInsets.ime.union(WindowInsets.navigationBars)

private val FooterHeight = 72.dp
private val FieldSpacing = 20.dp
private const val FADE_DURATION_MILLIS = 200

private class FormPreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawFormUM> {
    override val values: Sequence<TangemPayBankWithdrawFormUM> = sequenceOf(
        TangemPayBankWithdrawFormPreviewData.personalEmpty,
        TangemPayBankWithdrawFormPreviewData.personalWithSuggestions,
        TangemPayBankWithdrawFormPreviewData.personalWithError,
        TangemPayBankWithdrawFormPreviewData.personalFreeTextRegion,
        TangemPayBankWithdrawFormPreviewData.personalUsRegion,
        TangemPayBankWithdrawFormPreviewData.businessBank,
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TangemPayBankWithdrawFormScreen(
    @PreviewParameter(FormPreviewProvider::class) state: TangemPayBankWithdrawFormUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawFormScreen(state = state)
    }
}