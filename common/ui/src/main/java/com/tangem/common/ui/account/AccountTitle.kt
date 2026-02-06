package com.tangem.common.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.utils.StringsSigns

/**
 * A composable function that displays an account label (icon + name) with an optional prefix.
 *
 * Depending on the type of [accountTitleUM], it either shows a prefix text followed by
 * an account label (with name and icon) or just a title text.
 *
 * @param accountTitleUM The data model containing information about the account title.
 * @param modifier Optional [Modifier] for styling.
 * @param textStyle The [TextStyle] to apply to the text elements. Defaults to subtitle2 style from TangemTheme.
 */
@Composable
fun AccountTitle(
    accountTitleUM: AccountTitleUM,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TangemTheme.typography.subtitle2,
    textColor: Color = TangemTheme.colors.text.tertiary,
    iconSize: AccountIconSize = AccountIconSize.ExtraSmall,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        when (accountTitleUM) {
            is AccountTitleUM.Account -> {
                Text(
                    text = accountTitleUM.prefixText.resolveReference(),
                    style = textStyle,
                    color = textColor,
                )
                AccountLabel(
                    name = accountTitleUM.name,
                    icon = accountTitleUM.icon,
                    iconSize = iconSize,
                    nameStyle = textStyle,
                    nameColor = textColor,
                )
            }
            is AccountTitleUM.Text -> Text(
                text = accountTitleUM.title.resolveReference(),
                style = textStyle,
                color = textColor,
                modifier = Modifier.testTag(SendScreenTestTags.AMOUNT_CONTAINER_TITLE),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewAccountTitle() {
    TangemThemePreview {
        Column {
            AccountTitle(
                accountTitleUM = AccountTitleUM.Account(
                    prefixText = stringReference(StringsSigns.DOT),
                    name = stringReference("Main Wallet"),
                    icon = AccountIconUM.CryptoPortfolio(
                        value = CryptoPortfolioIcon.Icon.Bookmark,
                        color = CryptoPortfolioIcon.Color.Pattypan,
                    ),
                ),
                modifier = Modifier.padding(4.dp),
            )
            AccountTitle(
                accountTitleUM = AccountTitleUM.Account.payment(prefixText = stringReference(StringsSigns.DOT)),
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}