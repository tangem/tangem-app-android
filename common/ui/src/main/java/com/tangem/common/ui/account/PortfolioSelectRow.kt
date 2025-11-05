package com.tangem.common.ui.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.account.AccountName

@Composable
fun PortfolioSelectRow(
    state: PortfolioSelectUM,
    modifier: Modifier = Modifier,
    leftContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(enabled = state.isMultiChoice, onClick = state.onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leftContent()
        val leftText = if (state.isAccountMode) R.string.account_details_title else R.string.wc_common_wallet
        Text(
            modifier = Modifier.weight(1f),
            text = stringResourceSafe(leftText),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerW12()
        if (state.icon != null) {
            AccountIcon(
                name = state.name,
                icon = state.icon,
                size = AccountIconSize.Small,
            )
        }
        Text(
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
            text = state.name.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        if (state.isMultiChoice) {
            Icon(
                modifier = Modifier
                    .size(width = 18.dp, height = 24.dp),
                painter = painterResource(id = R.drawable.ic_select_18_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Immutable
data class PortfolioSelectUM(
    val icon: CryptoPortfolioIconUM?,
    val name: TextReference,
    val isAccountMode: Boolean,
    val isMultiChoice: Boolean,
    val onClick: () -> Unit,
)

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortfolioSelectRowPreview(@PreviewParameter(PreviewProvider::class) state: PortfolioSelectUM) {
    TangemThemePreview {
        PortfolioSelectRow(
            state = state,
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
        )
    }
}

private class PreviewProvider : PreviewParameterProvider<PortfolioSelectUM> {

    override val values: Sequence<PortfolioSelectUM>
        get() = sequenceOf(PortfolioSelectRowPreviewData.account, PortfolioSelectRowPreviewData.wallet)
}

object PortfolioSelectRowPreviewData {
    val account
        get() = PortfolioSelectUM(
            icon = AccountIconPreviewData.randomAccountIcon(),
            name = AccountName.DefaultMain.toUM().value,
            isAccountMode = true,
            isMultiChoice = true,
            onClick = {},
        )
    val wallet
        get() = PortfolioSelectUM(
            icon = null,
            name = stringReference("Wallet Name"),
            isMultiChoice = false,
            isAccountMode = false,
            onClick = {},
        )
}