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
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.AddCustomTokenMode
import com.tangem.features.managetokens.component.preview.PreviewAddCustomTokenComponent
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenUM
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork

@Composable
internal fun AddCustomTokenBottomSheet(config: TangemBottomSheetConfig, content: @Composable (Modifier) -> Unit) {
    TangemBottomSheet<AddCustomTokenUM>(
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
private fun Title(model: AddCustomTokenUM, modifier: Modifier = Modifier) {
    TangemTopAppBar(
        modifier = modifier,
        title = model.title,
        titleAlignment = Alignment.CenterHorizontally,
        startButton = TopAppBarButtonUM.Back(model.popBack).takeIf { model.showBackButton },
        height = TangemTopAppBarHeight.BOTTOM_SHEET,
    )
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
    private val mode: AddCustomTokenMode get() = AddCustomTokenMode.Wallet(UserWalletId(stringValue = "321"))
    override val values: Sequence<AddCustomTokenComponent>
        get() = sequenceOf(
            PreviewAddCustomTokenComponent(),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenConfig(
                    mode = mode,
                    step = AddCustomTokenConfig.Step.FORM,
                    selectedNetwork = SelectedNetwork(
                        id = Network.ID(value = "1", derivationPath = Network.DerivationPath.None),
                        name = "Ethereum",
                        derivationPath = Network.DerivationPath.None,
                        canHandleTokens = false,
                    ),
                ),
            ),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenConfig(
                    mode = mode,
                    step = AddCustomTokenConfig.Step.NETWORK_SELECTOR,
                    selectedNetwork = SelectedNetwork(
                        id = Network.ID(value = "0", derivationPath = Network.DerivationPath.None),
                        name = "Ethereum",
                        derivationPath = Network.DerivationPath.None,
                        canHandleTokens = false,
                    ),
                ),
            ),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenConfig(
                    mode = mode,
                    step = AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR,
                    selectedDerivationPath = SelectedDerivationPath(
                        id = Network.ID(value = "0", derivationPath = Network.DerivationPath.None),
                        value = Network.DerivationPath.None,
                        name = "Ethereum",
                        isDefault = false,
                    ),
                ),
            ),
        )
}
// endregion Preview