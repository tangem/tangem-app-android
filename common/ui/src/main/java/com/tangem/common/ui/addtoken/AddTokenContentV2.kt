package com.tangem.common.ui.addtoken

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.PortfolioSelectRowV2
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.button.PrimaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun AddTokenContentV2(state: AddTokenUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        TokenHeader(
            cryptoCurrencyIconState = state.tokenToAdd.iconState,
            tokenName = state.tokenToAdd.titleState,
        )
        Column {
            if (state.portfolio.isMultiChoice) {
                PortfolioSelectRowV2(
                    modifier = Modifier.background(
                        color = TangemTheme.colors2.surface.level3,
                        shape = RoundedCornerShape(TangemTheme.dimens2.x5),
                    ),
                    state = state.portfolio,
                )

                SpacerH(TangemTheme.dimens2.x2)
            }

            NetworkRow(state.network)
        }

        SpacerH(TangemTheme.dimens2.x4)

        AddButton(
            modifier = Modifier.fillMaxWidth(),
            state = state.button,
        )
    }
}

@Composable
private fun TokenHeader(
    cryptoCurrencyIconState: CurrencyIconState,
    tokenName: TokenItemState.TitleState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens2.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CurrencyIcon(
            state = cryptoCurrencyIconState,
            iconSize = 70.dp,
            shouldDisplayNetwork = false,
        )

        SpacerH(TangemTheme.dimens2.x4)

        when (tokenName) {
            is TokenItemState.TitleState.Content -> {
                Text(
                    text = tokenName.text.resolveReference(),
                    style = TangemTheme.typography2.headingSemibold28,
                    color = TangemTheme.colors2.text.neutral.primary,
                    textAlign = TextAlign.Center,
                )
            }
            TokenItemState.TitleState.Loading -> {
                RectangleShimmer(
                    modifier = Modifier.size(width = TangemTheme.dimens2.x17, height = TangemTheme.dimens2.x9),
                    radius = TangemTheme.dimens2.x25,
                )
            }
            TokenItemState.TitleState.Locked -> {
                Box(
                    modifier = Modifier.background(
                        color = TangemTheme.colors2.surface.level4,
                        shape = RoundedCornerShape(TangemTheme.dimens2.x25),
                    ),
                )
            }
        }
    }
}

@Composable
private fun NetworkRow(state: AddTokenUM.Network, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier
            .clickable(enabled = state.editable, onClick = state.onClick)
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            ),
    ) {
        Icon(
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .size(TangemTheme.dimens2.x10)
                .padding(end = TangemTheme.dimens2.x3),
            tint = Color.Unspecified,
            imageVector = ImageVector.vectorResource(id = state.icon),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            text = stringResourceSafe(R.string.wc_common_network),
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.secondary,
        )
        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = state.name.resolveReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors2.text.neutral.primary,
        )
        if (state.editable) {
            Icon(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.TAIL)
                    .size(TangemTheme.dimens2.x5),
                painter = painterResource(id = com.tangem.common.ui.R.drawable.ic_select_choice_20),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.secondary,
            )
        }
    }
}

@Composable
private fun AddButton(state: AddTokenUM.Button, modifier: Modifier = Modifier) {
    val isIconExists = state.isEnabled && state.isTangemIconVisible
    PrimaryTangemButton(
        modifier = modifier,
        text = state.text,
        onClick = state.onConfirmClick,
        tangemIconUM = if (isIconExists) {
            TangemIconUM.Icon(
                iconRes = R.drawable.ic_tangem_24,
                tintReference = {
                    if (state.isEnabled) {
                        TangemTheme.colors2.graphic.neutral.primaryInverted
                    } else {
                        TangemTheme.colors2.graphic.neutral.quaternary
                    }
                },
            )
        } else {
            null
        },
        iconPosition = TangemButtonIconPosition.Start,
        isEnabled = state.isEnabled,
        size = TangemButtonSize.X12,
        shape = TangemButtonShape.Rounded,
        isLoading = state.showProgress,
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PreviewProvider::class) state: AddTokenUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level2)
                    .padding(horizontal = 16.dp),
            ) {
                AddTokenContentV2(state = state)
            }
        }
    }
}