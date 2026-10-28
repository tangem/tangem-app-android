package com.tangem.features.jointaccount.creation.config.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.picker.AccountColorPicker
import com.tangem.common.ui.account.picker.AccountIconPicker
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_16
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.jointaccount.impl.R
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun JointAccountConfigScreen(
    state: JointAccountConfigUM,
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .onSizeChanged { size -> footerHeightPx = size.height },
                isEnabled = state.isContinueEnabled,
                onContinueClick = state.onContinueClick,
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
            NameCard(state = state)

            ConfigCard {
                AccountColorPicker(
                    selectedColor = state.icon.color,
                    colors = state.colors,
                    onColorClick = state.onColorClick,
                )
            }

            ConfigCard {
                AccountIconPicker(
                    selectedIcon = state.icon.value,
                    icons = state.icons,
                    onIconClick = state.onIconClick,
                )
            }

            state.wallet?.let { wallet -> WalletCard(wallet = wallet) }
        }
    }
}

@Composable
private fun ConfigCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun NameCard(state: JointAccountConfigUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val defaultName = state.namePlaceholder.resolveReference()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Seeds the field with the placeholder text so Continue is enabled without typing; skipped once a
        // draft (own input or a restored one) already has a value. isValuePasted = true moves the cursor to
        // the end of the seeded text instead of leaving it at the start.
        if (state.name.isEmpty()) state.onNameChange(defaultName, true)
    }
    ConfigCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            AccountIcon(
                name = accountDisplayName(state = state),
                icon = state.icon,
                size = AccountIconSize.Large,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.account_form_name),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )

            SimpleTextField(
                modifier = Modifier
                    .testTag(JointAccountCreationTestTags.CONFIG_NAME_FIELD)
                    .focusRequester(focusRequester),
                centered = true,
                textStyle = TangemTheme.typography3.heading.medium,
                placeholder = state.namePlaceholder,
                value = state.name,
                singleLine = true,
                onValueChange = { state.onNameChange(it, false) },
                isValuePasted = state.isValuePasted,
                onValuePastedTriggerDismiss = state.onValuePastedTriggerDismiss,
            )
        }
    }
}

@Composable
private fun WalletCard(wallet: JointAccountConfigUM.WalletUM) {
    TangemRow(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
        onClick = wallet.onClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(text = resourceReference(R.string.common_wallet), role = TangemRowTextRole.Title)
        },
        valueSlot = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_key_card_20),
                    tint = TangemTheme.colors3.icon.secondary,
                    contentDescription = null,
                )

                TangemRowText(text = stringReference(value = wallet.name), role = TangemRowTextRole.Value)
            }
        },
        endSlot = {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.ic_chevron_down_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        },
    )
}

@Composable
private fun Footer(isEnabled: Boolean, onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemFooter(modifier = modifier) {
        TangemButton(
            modifier = Modifier
                .testTag(JointAccountCreationTestTags.CONFIG_CONTINUE_BUTTON)
                .fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_continue),
            isEnabled = isEnabled,
            onClick = onContinueClick,
        )
    }
}

/** The letter icon renders the account's initial, so it needs the name the user will end up with */
@Composable
private fun accountDisplayName(state: JointAccountConfigUM) = if (state.name.isBlank()) {
    state.namePlaceholder
} else {
    stringReference(value = state.name.trim())
}

@Preview(showBackground = true, heightDp = 874)
@Preview(showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountConfigScreen(
    @PreviewParameter(JointAccountConfigStateProvider::class) state: JointAccountConfigUM,
) {
    TangemThemePreviewRedesign {
        JointAccountConfigScreen(state = state, onCloseClick = {})
    }
}

private class JointAccountConfigStateProvider : CollectionPreviewParameterProvider<JointAccountConfigUM>(
    collection = listOf(
        // Single wallet: the selector row is absent
        createPreviewState(wallet = null),
        createPreviewState(
            wallet = JointAccountConfigUM.WalletUM(name = "My wallet", onClick = {}),
        ),
    ),
)

private fun createPreviewState(wallet: JointAccountConfigUM.WalletUM?): JointAccountConfigUM = JointAccountConfigUM(
    name = "",
    namePlaceholder = stringReference(value = "Joint account"),
    icon = AccountIconUM.CryptoPortfolio(
        value = CryptoPortfolioIcon.Icon.Family,
        color = CryptoPortfolioIcon.Color.Pattypan,
    ),
    colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
    icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
    wallet = wallet,
    isContinueEnabled = true,
    isValuePasted = false,
    onValuePastedTriggerDismiss = {},
    onNameChange = { _, _ -> },
    onColorClick = {},
    onIconClick = {},
    onContinueClick = {},
    onBackClick = {},
)