package com.tangem.common.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Displays account name with icon
 *
 * @param name account name
 * @param icon portfolio account icon model
 * @param iconSize portfolio account icon size
 * @param nameStyle account name style
 * @param nameColor account name color
 * @see AccountIcon
 */
@Composable
fun AccountLabel(
    name: TextReference,
    icon: AccountIconUM,
    iconSize: AccountIconSize,
    modifier: Modifier = Modifier,
    nameStyle: TextStyle = TangemTheme.typography.subtitle2,
    nameColor: Color = TangemTheme.colors.text.tertiary,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AccountIcon(
            name = name,
            icon = icon,
            size = iconSize,
        )
        Text(
            text = name.resolveReference(),
            style = nameStyle,
            color = nameColor,
            maxLines = 1,
        )
    }
}