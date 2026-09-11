package com.tangem.features.tangempay.withdraw.bank.form.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.common.TextInput
import com.tangem.features.tangempay.common.TextInputState
import com.tangem.features.tangempay.common.revealAboveFooter
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.FieldUM

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.fields.fastForEach { field -> FormField(state = field) }
        }
    }
}

@Composable
private fun FormField(state: FieldUM) {
    val focusManager = LocalFocusManager.current
    val error = state.error
    TextInput(
        label = state.field.label,
        value = state.value,
        onValueChange = state.onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .revealAboveFooter(footerHeight = FooterHeight),
        state = if (error != null) TextInputState.Error(text = error) else TextInputState.Default,
        placeholder = state.field.placeholder,
        keyboardOptions = KeyboardOptions(
            capitalization = state.field.capitalization,
            keyboardType = state.field.keyboardType,
            imeAction = state.imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        onFocusChange = state.onFocusChange,
    )
}

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

private class FormPreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawFormUM> {
    override val values: Sequence<TangemPayBankWithdrawFormUM> = sequenceOf(
        TangemPayBankWithdrawFormPreviewData.personalEmpty,
        TangemPayBankWithdrawFormPreviewData.personalWithError,
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