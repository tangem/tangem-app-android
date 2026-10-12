package com.tangem.features.tangempay.withdraw.bank.form.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.SuggestionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val CapsuleSpacing = 4.dp
private val CapsulePadding = 14.dp
private val CapsuleContentSpacing = 8.dp
private val ChevronSize = 20.dp
private const val NAME_SEPARATOR = " · "

/** Saved recipients matching the account number being typed; tapping one replaces the recipient being entered. */
@Composable
internal fun BankAccountSuggestions(suggestions: ImmutableList<SuggestionUM>, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CapsuleSpacing),
    ) {
        suggestions.fastForEach { suggestion ->
            key(suggestion.id) {
                SuggestionCapsule(
                    suggestion = suggestion,
                    onClick = {
                        focusManager.clearFocus()
                        suggestion.onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun SuggestionCapsule(suggestion: SuggestionUM, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.secondary)
            .clickableSingle(role = Role.Button, onClick = onClick)
            .padding(CapsulePadding),
        horizontalArrangement = Arrangement.spacedBy(CapsuleContentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = suggestion.asAnnotatedString(),
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TangemIcon(
            modifier = Modifier.size(ChevronSize),
            tangemIconUM = TangemIconUM.Icon(
                imageVector = Icons.ic_chevron_right_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}

@Composable
private fun SuggestionUM.asAnnotatedString(): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = TangemTheme.colors3.text.brand)) {
        append(matchedPrefix)
    }
    withStyle(SpanStyle(color = TangemTheme.colors3.text.primary)) {
        append(accountNumberSuffix)
    }
    withStyle(SpanStyle(color = TangemTheme.colors3.text.secondary)) {
        append(NAME_SEPARATOR)
        append(name)
    }
}

@Preview(widthDp = 402, showBackground = true)
@Preview(widthDp = 402, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_BankAccountSuggestions() {
    TangemThemePreviewRedesign {
        BankAccountSuggestions(
            suggestions = persistentListOf(
                SuggestionUM(
                    id = "1",
                    matchedPrefix = "1234",
                    accountNumberSuffix = "56789012",
                    name = "John D.",
                    onClick = {},
                ),
                SuggestionUM(
                    id = "2",
                    matchedPrefix = "1234",
                    accountNumberSuffix = "99887766",
                    name = "Arasaka Corporation",
                    onClick = {},
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}