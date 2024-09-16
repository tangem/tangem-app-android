package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.TangemTopAppBarHeight
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.preview.PreviewAddCustomTokenComponent
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.impl.R

@Composable
internal fun AddCustomTokenBottomSheet(config: TangemBottomSheetConfig, content: @Composable (Modifier) -> Unit) {
    TangemBottomSheet<AddCustomTokenConfig>(
        config = config,
        addBottomInsets = false,
        title = { model ->
            Title(model)
        },
        containerColor = TangemTheme.colors.background.secondary,
        content = {
            val contentModifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxSize()

            content(contentModifier)
        },
    )
}

@Composable
private fun Title(model: AddCustomTokenConfig, modifier: Modifier = Modifier) {
    when (model.step) {
        AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
        AddCustomTokenConfig.Step.FORM,
        -> {
            TangemBottomSheetTitle(
                modifier = modifier,
                title = resourceReference(R.string.add_custom_token_title),
            )
        }
        AddCustomTokenConfig.Step.NETWORK_SELECTOR -> {
            TangemTopAppBar(
                modifier = modifier,
                title = resourceReference(R.string.custom_token_network_selector_title),
                titleAlignment = Alignment.CenterHorizontally,
                startButton = TopAppBarButtonUM.Back(model.popBack),
                height = TangemTopAppBarHeight.BOTTOM_SHEET,
            )
        }
        AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR -> {
            TangemTopAppBar(
                modifier = modifier,
                title = resourceReference(R.string.custom_token_derivation_path),
                titleAlignment = Alignment.CenterHorizontally,
                startButton = TopAppBarButtonUM.Back(model.popBack),
                height = TangemTopAppBarHeight.BOTTOM_SHEET,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_AddCustomTokenBottomSheet(
    @PreviewParameter(AddCustomTokenComponentPreviewProvider::class) component: AddCustomTokenComponent,
) {
    TangemThemePreview {
        component.BottomSheet()
    }
}

private class AddCustomTokenComponentPreviewProvider : PreviewParameterProvider<AddCustomTokenComponent> {
    override val values: Sequence<AddCustomTokenComponent>
        get() = sequenceOf(
            PreviewAddCustomTokenComponent(),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenConfig(
                    userWalletId = UserWalletId(stringValue = "321"),
                    step = AddCustomTokenConfig.Step.FORM,
                    popBack = {},
                    selectedNetwork = SelectedNetwork(
                        id = Network.ID(value = "1"),
                        name = stringReference("Ethereum"),
                        derivationPath = Network.DerivationPath.None,
                        canHandleTokens = false,
                    ),
                ),
            ),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenConfig(
                    userWalletId = UserWalletId(stringValue = "321"),
                    step = AddCustomTokenConfig.Step.NETWORK_SELECTOR,
                    popBack = {},
                    selectedNetwork = SelectedNetwork(
                        id = Network.ID(value = "0"),
                        name = stringReference("Ethereum"),
                        derivationPath = Network.DerivationPath.None,
                        canHandleTokens = false,
                    ),
                ),
            ),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenConfig(
                    userWalletId = UserWalletId(stringValue = "321"),
                    step = AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR,
                    popBack = {},
                    selectedDerivationPath = SelectedDerivationPath(
                        id = Network.ID(value = "0"),
                        value = Network.DerivationPath.None,
                        networkName = stringReference("Ethereum"),
                    ),
                ),
            ),
        )
}
// endregion Preview