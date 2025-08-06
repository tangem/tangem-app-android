package com.tangem.features.account.createedit.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.fields.AutoSizeTextField
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.features.account.createedit.entity.AccountCreateEditUM.Account
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongMethod", "MagicNumber")
@Composable
internal fun AccountCreateEditContent(state: AccountCreateEditUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            text = state.title.resolveReference(),
            onBackClick = state.onCloseClick,
            iconRes = R.drawable.ic_close_24,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),

        ) {
            AccountSummary(state.account, state.account.onNameChange)
            SpacerH24()
            AccountColor(state.colorsState)
            SpacerH24()
            AccountIcon(state.iconsState)
            SpacerH8()
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = state.account.derivationInfo.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = state.buttonState.isButtonEnabled,
            text = state.buttonState.text.resolveReference(),
            onClick = state.buttonState.onConfirmClick,
        )
    }
}

@Composable
private fun AccountSummary(account: AccountCreateEditUM.Account, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors.background.action),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        AccountIcon(account)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResourceSafe(R.string.account_form_name),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        Spacer(modifier = Modifier.height(2.dp))

        AutoSizeTextField(
            centered = true,
            textStyle = TangemTheme.typography.head,
            placeholder = account.inputPlaceholder,
            value = account.name.resolveReference(),
            singleLine = true,
            onValueChange = onNameChange,
        )
        SpacerH(20.dp)
    }
}

@Composable
private fun AccountIcon(account: AccountCreateEditUM.Account) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(TangemTheme.dimens.radius24))
            .background(account.portfolioIcon.color.getUiColor()),
    ) {
        val icon = account.portfolioIcon.value
        when {
            icon == CryptoPortfolioIcon.Icon.Letter -> Text(
                text = account.name.resolveReference().first().uppercase(),
                style = TangemTheme.typography.head,
                color = TangemTheme.colors.text.constantWhite,
            )
            else -> Icon(
                modifier = Modifier.size(44.dp),
                tint = TangemTheme.colors.text.constantWhite,
                imageVector = ImageVector.vectorResource(id = icon.getResId()),
                contentDescription = null,
            )
        }
    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun AccountColor(colorsState: AccountCreateEditUM.Colors) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors.background.action),
    ) {
        val columns = GridCells.Fixed(6)
        val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        LazyVerticalGrid(
            columns = columns,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(colorsState.list) { index, color ->
                val isSelected = color == colorsState.selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .wrapContentSize()
                        .clickable(onClick = { colorsState.onColorSelect(color) })
                        .size(48.dp),
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(47.dp)
                                .border(2.dp, color.getUiColor(), shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color = color.getUiColor(), shape = CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color = color.getUiColor(), shape = CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun AccountIcon(iconsState: AccountCreateEditUM.Icons) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors.background.action)
            .padding(8.dp),
    ) {
        val columns = GridCells.Fixed(6)
        LazyVerticalGrid(
            columns = columns,
        ) {
            itemsIndexed(iconsState.list) { index, icon ->
                val isSelected = icon == iconsState.selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .wrapContentSize()
                        .clickable(onClick = { iconsState.onIconSelect(icon) })
                        .size(52.dp),
                ) {
                    if (isSelected) {
                        val borderColor: Color
                        val iconTint: Color
                        val backgroundTint: Color
                        if (index == 0) {
                            borderColor = TangemTheme.colors.icon.accent
                            iconTint = TangemTheme.colors.icon.accent
                            backgroundTint = TangemTheme.colors.icon.accent.copy(alpha = 0.1f)
                        } else {
                            borderColor = TangemTheme.colors.icon.informative
                            iconTint = TangemTheme.colors.icon.secondary
                            backgroundTint = TangemTheme.colors.field.focused
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .border(2.dp, borderColor, shape = CircleShape),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color = backgroundTint, shape = CircleShape),
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = icon.getResId()),
                                    contentDescription = null,
                                    tint = iconTint,
                                )
                            }
                        }
                    } else {
                        val iconTint: Color
                        val backgroundTint: Color
                        if (index == 0) {
                            iconTint = TangemTheme.colors.icon.accent
                            backgroundTint = TangemTheme.colors.icon.accent.copy(alpha = 0.1f)
                        } else {
                            iconTint = TangemTheme.colors.text.tertiary
                            backgroundTint = TangemTheme.colors.field.focused
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(color = backgroundTint, shape = CircleShape),
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = icon.getResId()),
                                contentDescription = null,
                                tint = iconTint,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcConnectionsContentPreview(@PreviewParameter(PreviewStateProvider::class) params: AccountCreateEditUM) {
    TangemThemePreview {
        AccountCreateEditContent(state = params)
    }
}

private class PreviewStateProvider : CollectionPreviewParameterProvider<AccountCreateEditUM>(
    buildList {
        val colors = CryptoPortfolioIcon.Color.entries.toImmutableList()
        val icons = CryptoPortfolioIcon.Icon.entries.toImmutableList()
        var portfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount()
        val first = AccountCreateEditUM(
            title = stringReference("Add account"),
            account = Account(
                portfolioIcon = portfolioIcon,
                derivationInfo = stringReference("Account #03 — used for address derivation."),
            ),
            colorsState = AccountCreateEditUM.Colors(
                selected = portfolioIcon.color,
                list = colors.toImmutableList(),
            ),
            iconsState = AccountCreateEditUM.Icons(
                selected = portfolioIcon.value,
                list = icons.toImmutableList(),
            ),
            buttonState = AccountCreateEditUM.Button(
                text = stringReference("Add account"),
            ),
        )
        add(first)

        portfolioIcon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Letter,
            color = CryptoPortfolioIcon.Color.entries.random(),
        )
        val second = AccountCreateEditUM(
            title = stringReference("Edit account"),
            account = Account(
                portfolioIcon = portfolioIcon,
                name = stringReference("Main account"),
                derivationInfo = stringReference("Account #03 — used for address derivation."),
            ),
            colorsState = AccountCreateEditUM.Colors(
                selected = portfolioIcon.color,
                list = colors.toImmutableList(),
            ),
            iconsState = AccountCreateEditUM.Icons(
                selected = portfolioIcon.value,
                list = icons.toImmutableList(),
            ),
            buttonState = AccountCreateEditUM.Button(
                text = stringReference("Save"),
            ),
        )
        add(second)
    },
)