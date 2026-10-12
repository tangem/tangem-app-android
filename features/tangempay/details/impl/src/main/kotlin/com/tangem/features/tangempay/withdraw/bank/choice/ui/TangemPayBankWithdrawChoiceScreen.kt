package com.tangem.features.tangempay.withdraw.bank.choice.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUM
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUM.OptionUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayBankWithdrawChoiceScreen(state: TangemPayBankWithdrawChoiceUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        TangemTopNavigation(
            title = state.header.title,
            subtitle = state.header.subtitle,
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onBack = state.onBackClick,
            onClose = state.header.onCloseClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = state.sectionTitle.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            OptionsCard(options = state.options)
        }
        TangemFooter(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                isEnabled = state.isContinueEnabled,
                text = resourceReference(R.string.common_continue),
                onClick = state.onContinueClick,
            )
        }
    }
}

@Composable
private fun OptionsCard(options: ImmutableList<OptionUM>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCornerRadius))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        options.fastForEachIndexed { index, option ->
            TangemRow(
                divider = index < options.lastIndex,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                titleSlot = { TangemRowText(text = option.title, role = TangemRowTextRole.Title) },
                endSlot = { TangemCheckmark(checked = option.isSelected, onCheckedChange = null) },
                onClick = option.onClick,
            )
        }
    }
}

private val CardCornerRadius = 24.dp

private class ChoicePreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawChoiceUM> {
    override val values: Sequence<TangemPayBankWithdrawChoiceUM> = sequenceOf(
        TangemPayBankWithdrawChoicePreviewData.transferTypeUnanswered,
        TangemPayBankWithdrawChoicePreviewData.transferType,
        TangemPayBankWithdrawChoicePreviewData.accountType,
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TangemPayBankWithdrawChoiceScreen(
    @PreviewParameter(ChoicePreviewProvider::class) state: TangemPayBankWithdrawChoiceUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawChoiceScreen(state = state)
    }
}