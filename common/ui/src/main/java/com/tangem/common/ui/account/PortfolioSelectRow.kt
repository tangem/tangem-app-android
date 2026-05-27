package com.tangem.common.ui.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
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

@Composable
fun PortfolioSelectRowV2(
    state: PortfolioSelectUM,
    modifier: Modifier = Modifier,
    leftContent: @Composable RowScope.() -> Unit = {},
) {
    val leftText = if (state.isAccountMode) R.string.account_details_title else R.string.wc_common_wallet
    TangemRowContainer(
        modifier.clickable(enabled = state.isMultiChoice, onClick = state.onClick),
    ) {
        if (state.icon != null) {
            Box(
                modifier = Modifier.layoutId(TangemRowLayoutId.HEAD),
                contentAlignment = Alignment.Center,
            ) {
                AccountIcon(
                    modifier = Modifier.padding(end = TangemTheme.dimens2.x3),
                    name = state.name,
                    icon = state.icon,
                    size = AccountIconSize.RedesignedDefault,
                )
            }
        }

        Row(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            verticalAlignment = Alignment.Bottom,
        ) {
            leftContent()
            Text(
                modifier = Modifier.weight(1f),
                text = stringResourceSafe(leftText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.neutral.secondary,
            )
        }

        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = state.name.resolveReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors2.text.neutral.primary,
        )
        if (state.isMultiChoice) {
            Icon(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.TAIL)
                    .size(TangemTheme.dimens2.x5),
                painter = painterResource(id = R.drawable.ic_select_choice_20),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.secondary,
            )
        }
    }
}

@Immutable
data class PortfolioSelectUM(
    val icon: AccountIconUM?,
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

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortfolioSelectRowPreviewV2(@PreviewParameter(PreviewProvider::class) state: PortfolioSelectUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            PortfolioSelectRowV2(
                state = state,
                modifier = Modifier.background(TangemTheme.colors2.surface.level3),
            )
        }
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