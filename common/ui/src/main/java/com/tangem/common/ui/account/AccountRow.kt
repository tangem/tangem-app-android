package com.tangem.common.ui.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Displays a row representing an account with an icon, title, and subtitle.
 *
 * The row consists of:
 * - An [AccountIcon] on the left.
 * - A column with the [title] and [subtitle] texts, which can be displayed in normal
 *   or reversed order depending on [isReverse].
 *
 * The layout uses horizontal spacing between the icon and text, and vertical spacing
 * between the title and subtitle.
 *
 * @param title The main text shown in the row, usually representing the account name.
 * @param subtitle The secondary text, typically providing additional details about the account.
 * @param icon The account icon definition, displayed using [AccountIcon].
 * @param isReverse If `true`, the [subtitle] is displayed above the [title].
 * Otherwise, the [title] is displayed above the [subtitle].
 */
@Composable
fun AccountRow(
    title: TextReference,
    subtitle: TextReference,
    icon: CryptoPortfolioIconUM,
    modifier: Modifier = Modifier,
    isReverse: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AccountIcon(
            name = title,
            icon = icon,
            size = AccountIconSize.Default,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
        ) {
            if (isReverse) {
                Subtitle(subtitle)
                Title(title)
            } else {
                Title(title)
                Subtitle(subtitle)
            }
        }
    }
}

@Composable
private fun Title(title: TextReference) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.primary1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Subtitle(subtitle: TextReference) {
    Text(
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.caption2,
        text = subtitle.resolveReference(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Sample()
    }
}

@Composable
private fun Sample() {
    val name = stringReference("Main account")
    val info = stringReference("10 tokens in 2 networks")
    val subtitle = resourceReference(R.string.account_form_name)
    fun icon(letter: Boolean = false) = AccountIconPreviewData.randomAccountIcon(letter)
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        AccountRow(title = name, subtitle = info, icon = icon())
        AccountRow(title = name, subtitle = subtitle, icon = icon(), isReverse = true)
    }
}