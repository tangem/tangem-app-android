package com.tangem.common.ui.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconPreviewData.randomAccountIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.CryptoPortfolioIcon.Color

enum class AccountIconSize {
    Default, Large, Medium, Small, ExtraSmall
}

/**
 * Displays an account icon that can either show a letter (derived from [name])
 * or a predefined vector resource (from [icon]).
 *
 * The background color is determined by the icon's [CryptoPortfolioIconUM.color],
 * and the icon size, text style, and box modifier are adapted based on the given [size].
 *
 * @param name The text reference used to resolve and display the first letter
 * when [icon] is set to [CryptoPortfolioIcon.Icon.Letter].
 * @param icon The account icon definition, which can be a letter or a drawable resource.
 * @param size The size of the icon, defined by [AccountIconSize].
 */
@Composable
fun AccountIcon(
    name: TextReference,
    icon: CryptoPortfolioIconUM,
    size: AccountIconSize,
    modifier: Modifier = Modifier,
) {
    val boxModifier = modifier.selectBoxModifier(size)
    val iconSize = Modifier.selectIconSize(size)
    val textStyle = when (size) {
        AccountIconSize.Default -> TangemTheme.typography.h3
        AccountIconSize.Large -> TangemTheme.typography.h1
        AccountIconSize.Medium -> TangemTheme.typography.subtitle1
        AccountIconSize.Small -> TangemTheme.typography.subtitle2
        AccountIconSize.ExtraSmall -> TangemTheme.typography.caption1
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = boxModifier.background(icon.color.getUiColor()),
    ) {
        val icon = icon.value
        val letter = name.resolveReference().firstOrNull()
        when {
            icon == CryptoPortfolioIcon.Icon.Letter -> Text(
                text = letter?.uppercase() ?: "",
                style = textStyle,
                color = TangemTheme.colors.text.constantWhite,
            )
            else -> Icon(
                modifier = iconSize,
                tint = TangemTheme.colors.text.constantWhite,
                imageVector = ImageVector.vectorResource(id = icon.getResId()),
                contentDescription = null,
            )
        }
    }
}

private fun Modifier.selectIconSize(size: AccountIconSize): Modifier = when (size) {
    AccountIconSize.Default -> this.size(20.dp)
    AccountIconSize.Large -> this.size(40.dp)
    AccountIconSize.Medium -> this.size(16.dp)
    AccountIconSize.Small -> this.size(12.dp)
    AccountIconSize.ExtraSmall -> this.size(8.dp)
}

private fun Modifier.selectBoxModifier(size: AccountIconSize): Modifier = when (size) {
    AccountIconSize.Default -> size(36.dp).clip(RoundedCornerShape(10.dp))
    AccountIconSize.Large -> size(88.dp).clip(RoundedCornerShape(24.dp))
    AccountIconSize.Medium -> size(28.dp).clip(RoundedCornerShape(8.dp))
    AccountIconSize.Small -> size(20.dp).clip(RoundedCornerShape(6.dp))
    AccountIconSize.ExtraSmall -> size(14.dp).clip(RoundedCornerShape(4.dp))
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AccountIcon() {
    TangemThemePreview {
        Sample()
    }
}

@Composable
private fun Sample() {
    val name = stringReference("Account Name")
    Row(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            AccountIcon(name = name, randomAccountIcon(), size = AccountIconSize.Default)
            AccountIcon(name = name, randomAccountIcon(), size = AccountIconSize.Large)
            AccountIcon(name = name, randomAccountIcon(), size = AccountIconSize.Medium)
            AccountIcon(name = name, randomAccountIcon(), size = AccountIconSize.Small)
            AccountIcon(name = name, randomAccountIcon(), size = AccountIconSize.ExtraSmall)
        }
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            AccountIcon(name = name, randomAccountIcon(letter = true), size = AccountIconSize.Default)
            AccountIcon(name = name, randomAccountIcon(letter = true), size = AccountIconSize.Large)
            AccountIcon(name = name, randomAccountIcon(letter = true), size = AccountIconSize.Medium)
            AccountIcon(name = name, randomAccountIcon(letter = true), size = AccountIconSize.Small)
            AccountIcon(name = name, randomAccountIcon(letter = true), size = AccountIconSize.ExtraSmall)
        }
    }
}

object AccountIconPreviewData {

    fun randomAccountIcon(letter: Boolean = false) = CryptoPortfolioIconUM(
        value = if (letter) CryptoPortfolioIcon.Icon.Letter else CryptoPortfolioIcon.Icon.entries.random(),
        color = Color.entries.random(),
    )
}