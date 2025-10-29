package com.tangem.common.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SendScreenTestTags

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