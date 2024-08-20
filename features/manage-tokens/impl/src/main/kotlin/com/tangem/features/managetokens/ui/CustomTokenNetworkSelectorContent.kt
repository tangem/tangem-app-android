package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.CustomTokenNetworkSelectorComponent
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenNetworkSelectorComponent
import com.tangem.features.managetokens.entity.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.CustomTokenNetworkSelectorUM
import com.tangem.features.managetokens.entity.SelectedNetworkUM
import com.tangem.features.managetokens.impl.R

internal fun LazyListScope.customTokenNetworkSelectorContent(model: CustomTokenNetworkSelectorUM) {
    val lastIndex = model.networks.lastIndex

    if (model.showTitle) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TangemTheme.dimens.size36)
                    .background(
                        color = TangemTheme.colors.background.primary,
                        shape = TangemTheme.shapes.bottomSheet,
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            top = TangemTheme.dimens.spacing12,
                            bottom = TangemTheme.dimens.spacing6,
                        )
                        .padding(horizontal = TangemTheme.dimens.spacing12),
                    text = stringResource(R.string.add_custom_token_choose_network),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }

    itemsIndexed(
        items = model.networks,
        key = { _, item -> item.id.value },
    ) { index, item ->
        NetworkItem(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = when {
                        !model.showTitle && index == 0 -> RoundedCornerShape(
                            topStart = TangemTheme.dimens.radius16,
                            topEnd = TangemTheme.dimens.radius16,
                        )
                        index == lastIndex -> RoundedCornerShape(
                            bottomStart = TangemTheme.dimens.radius16,
                            bottomEnd = TangemTheme.dimens.radius16,
                        )
                        else -> RectangleShape
                    },
                )
                .background(color = TangemTheme.colors.background.primary)
                .clickable(onClick = { item.onSelectedStateChange(true) })
                .padding(horizontal = TangemTheme.dimens.spacing4),
            model = item,
        )
    }
}

@Composable
private fun NetworkItem(model: CurrencyNetworkUM, modifier: Modifier = Modifier) {
    ChainRow(
        modifier = modifier,
        model = with(model) {
            ChainRowUM(
                name = name,
                type = type,
                icon = CurrencyIconState.CoinIcon(
                    url = null,
                    fallbackResId = model.iconResId,
                    isGrayscale = false,
                    showCustomBadge = false,
                ),
                showCustom = false,
            )
        },
        action = {
            AnimatedVisibility(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                visible = model.isSelected,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_24),
                    tint = TangemTheme.colors.icon.accent,
                    contentDescription = null,
                )
            }
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CustomTokenNetworkSelectorContent(
    @PreviewParameter(CustomTokenNetworkSelectorComponentPreviewProvider::class)
    component: CustomTokenNetworkSelectorComponent,
) {
    TangemThemePreview {
        LazyColumn {
            component.content(this)
        }
    }
}

private class CustomTokenNetworkSelectorComponentPreviewProvider :
    PreviewParameterProvider<CustomTokenNetworkSelectorComponent> {
    override val values: Sequence<CustomTokenNetworkSelectorComponent>
        get() = sequenceOf(
            PreviewCustomTokenNetworkSelectorComponent(),
            PreviewCustomTokenNetworkSelectorComponent(
                params = CustomTokenNetworkSelectorComponent.Params(
                    userWalletId = UserWalletId(stringValue = "321"),
                    selectedNetwork = SelectedNetworkUM(
                        id = Network.ID(value = "0"),
                        name = "",
                    ),
                    onNetworkSelected = {},
                ),
            ),
        )
}
// endregion Preview