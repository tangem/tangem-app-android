package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.bottomFade
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.managetokens.component.AddCustomTokenMode
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorUM
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.item.DerivationPathUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.component.AddCustomTokenDescription

@Composable
internal fun CustomTokenSelectorContent(model: CustomTokenSelectorUM, modifier: Modifier = Modifier) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val lastIndex = model.items.lastIndex

    LazyColumn(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .bottomFade(),
        contentPadding = PaddingValues(
            bottom = 96.dp + bottomBarHeight,
        ),
    ) {
        item {
            Header(model.header)
        }

        itemsIndexed(
            items = model.items,
            key = { _, item -> item.id },
        ) { index, item ->
            val itemModifier = Modifier
                .fillMaxWidth()
                .clip(
                    shape = when {
                        model.header !is CustomTokenSelectorUM.HeaderUM.Description && index == 0 -> {
                            RoundedCornerShape(
                                topStart = TangemTheme.dimens.radius16,
                                topEnd = TangemTheme.dimens.radius16,
                            )
                        }
                        index == lastIndex -> {
                            RoundedCornerShape(
                                bottomStart = TangemTheme.dimens.radius16,
                                bottomEnd = TangemTheme.dimens.radius16,
                            )
                        }
                        else -> {
                            RectangleShape
                        }
                    },
                )
                .background(color = TangemTheme.colors.background.primary)
                .clickable(onClick = { item.onSelectedStateChange(true) })
                .padding(horizontal = TangemTheme.dimens.spacing4)

            when (item) {
                is CurrencyNetworkUM -> {
                    NetworkItem(
                        modifier = itemModifier,
                        model = item,
                    )
                }
                is DerivationPathUM -> {
                    DerivationPathItem(
                        modifier = itemModifier,
                        model = item,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(header: CustomTokenSelectorUM.HeaderUM, modifier: Modifier = Modifier) {
    when (header) {
        is CustomTokenSelectorUM.HeaderUM.CustomDerivationButton -> {
            CustomDerivationButton(
                modifier = modifier.padding(vertical = TangemTheme.dimens.spacing16),
                enteredDerivationPath = header.value,
                isSelected = header.value != null,
                onClick = header.onClick,
            )
        }
        is CustomTokenSelectorUM.HeaderUM.Description -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AddCustomTokenDescription()
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(min = TangemTheme.dimens.size36)
                        .background(
                            color = TangemTheme.colors.background.primary,
                            shape = TangemTheme.shapes.bottomSheet,
                        ),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(top = TangemTheme.dimens.spacing12)
                            .padding(horizontal = TangemTheme.dimens.spacing12),
                        text = stringResourceSafe(R.string.add_custom_token_choose_network),
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }
        is CustomTokenSelectorUM.HeaderUM.None -> {
            Spacer(modifier = Modifier.size(TangemTheme.dimens.spacing16))
        }
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
            SelectedIcon(isVisible = model.isSelected)
        },
    )
}

@Composable
private fun CustomDerivationButton(
    enteredDerivationPath: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InformationBlock(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .clickable(onClick = onClick),
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
            ) {
                Text(
                    text = stringResourceSafe(id = R.string.custom_token_custom_derivation),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.secondary,
                )
                Text(
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                    text = enteredDerivationPath
                        ?: stringResourceSafe(id = R.string.custom_token_custom_derivation_title),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        },
        action = {
            SelectedIcon(isVisible = isSelected)
        },
    )
}

@Composable
private fun DerivationPathItem(model: DerivationPathUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        shape = RectangleShape,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
            ) {
                Text(
                    text = model.networkName.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.secondary,
                )

                Text(
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                    text = model.value,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        },
        action = {
            SelectedIcon(isVisible = model.isSelected)
        },
    )
}

@Composable
private fun SelectedIcon(isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier.size(TangemTheme.dimens.size24),
        visible = isVisible,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_24),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CustomTokenNetworkSelectorContent(
    @PreviewParameter(CustomTokenNetworkSelectorComponentPreviewProvider::class)
    component: CustomTokenSelectorComponent,
) {
    TangemThemePreview {
        component.Content(modifier = Modifier)
    }
}

private class CustomTokenNetworkSelectorComponentPreviewProvider :
    PreviewParameterProvider<CustomTokenSelectorComponent> {

    private val derivationPath = Network.DerivationPath.Card("m/44'/0'/0'/0/0")
    private val mode: AddCustomTokenMode get() = AddCustomTokenMode.Wallet(UserWalletId(stringValue = "321"))

    override val values: Sequence<CustomTokenSelectorComponent>
        get() = sequenceOf(
            PreviewCustomTokenSelectorComponent(
                params = CustomTokenSelectorComponent.Params.DerivationPathSelector(
                    mode = mode,
                    selectedNetwork = SelectedNetwork(
                        id = Network.ID(value = "0", derivationPath = derivationPath),
                        name = "Ethereum",
                        derivationPath = derivationPath,
                        canHandleTokens = true,
                    ),
                    selectedDerivationPath = SelectedDerivationPath(
                        id = Network.ID(value = "0", derivationPath = derivationPath),
                        value = derivationPath,
                        name = "",
                        isDefault = false,
                    ),
                    onDerivationPathSelected = {},
                ),
            ),
            PreviewCustomTokenSelectorComponent(
                params = CustomTokenSelectorComponent.Params.NetworkSelector(
                    mode = mode,
                    selectedNetwork = SelectedNetwork(
                        id = Network.ID(value = "0", derivationPath = derivationPath),
                        name = "Ethereum",
                        derivationPath = derivationPath,
                        canHandleTokens = true,
                    ),
                    onNetworkSelected = {},
                ),
            ),
        )
}
// endregion Preview