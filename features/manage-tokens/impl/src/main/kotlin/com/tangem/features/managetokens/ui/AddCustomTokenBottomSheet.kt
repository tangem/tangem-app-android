package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.TangemTopAppBarHeight
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.components.isOpened
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.preview.PreviewAddCustomTokenComponent
import com.tangem.features.managetokens.entity.AddCustomTokenButtonUM
import com.tangem.features.managetokens.entity.AddCustomTokenUM
import com.tangem.features.managetokens.entity.SelectedNetworkUM
import com.tangem.features.managetokens.impl.R

@Composable
internal fun AddCustomTokenBottomSheet(config: TangemBottomSheetConfig, content: LazyListScope.() -> Unit) {
    TangemBottomSheet<AddCustomTokenUM>(
        config = config,
        title = { model ->
            Title(model)
        },
        containerColor = TangemTheme.colors.background.secondary,
        content = { model ->
            Content(
                model = model,
                content = content,
            )
        },
    )
}

@Composable
private fun Title(model: AddCustomTokenUM, modifier: Modifier = Modifier) {
    val showTokenNetworkTitle = model is AddCustomTokenUM.NetworkSelector && model.selectedNetwork != null

    if (showTokenNetworkTitle) {
        TangemTopAppBar(
            modifier = modifier,
            title = resourceReference(R.string.custom_token_network_selector_title),
            titleAlignment = Alignment.CenterHorizontally,
            startButton = TopAppBarButtonUM.Back(model.popBack),
            height = TangemTopAppBarHeight.BOTTOM_SHEET,
        )
    } else {
        TangemBottomSheetTitle(
            modifier = modifier,
            title = resourceReference(R.string.add_custom_token_title),
        )
    }
}

@Composable
private fun Content(model: AddCustomTokenUM, content: LazyListScope.() -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val keyboardState by keyboardAsState()

    var fabHeight by remember { mutableStateOf(0.dp) }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = TangemTheme.colors.background.secondary,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier.onSizeChanged {
                    fabHeight = with(density) { it.height.toDp() }
                },
                visible = model.addTokenButton.isVisible && !keyboardState.isOpened,
                enter = fadeIn(),
                exit = fadeOut(),
                label = "Add button visibility",
            ) {
                PrimaryButton(
                    modifier = Modifier
                        .padding(bottom = TangemTheme.dimens.spacing16)
                        .padding(horizontal = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.custom_token_add_token),
                    enabled = model.addTokenButton.isEnabled,
                    onClick = model.addTokenButton.onClick,
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing32 + fabHeight,
            ),
        ) {
            item {
                if (model is AddCustomTokenUM.NetworkSelector && model.selectedNetwork != null) {
                    Spacer(modifier = Modifier.size(TangemTheme.dimens.spacing12))
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = TangemTheme.dimens.spacing16),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
                            text = stringResource(id = R.string.custom_token_subtitle),
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.secondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            content()
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
        component.BottomSheet(isVisible = true, onDismiss = {})
    }
}

private class AddCustomTokenComponentPreviewProvider : PreviewParameterProvider<AddCustomTokenComponent> {
    override val values: Sequence<AddCustomTokenComponent>
        get() = sequenceOf(
            PreviewAddCustomTokenComponent(),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenUM.NetworkSelector(
                    popBack = {},
                    selectedNetwork = SelectedNetworkUM(
                        id = Network.ID(value = "0"),
                        name = "Ethereum",
                    ),
                ),
            ),
            PreviewAddCustomTokenComponent(
                initialState = AddCustomTokenUM.Form(
                    popBack = {},
                    selectedNetwork = SelectedNetworkUM(
                        id = Network.ID(value = "1"),
                        name = "Ethereum",
                    ),
                    addTokenButton = AddCustomTokenButtonUM.Visible(
                        isEnabled = false,
                        onClick = {},
                    ),
                ),
            ),
        )
}
// endregion Preview
