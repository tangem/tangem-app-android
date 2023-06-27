package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet.TestTokenItem
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenTestBlock
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenFloatingButton
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenToolbar
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenWarnings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Add custom token content for testing
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun AddCustomTokenTestContent(state: AddCustomTokenStateHolder.TestContent, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(initialValue = BottomSheetValue.Collapsed),
    )

    BackHandler(
        onBack = {
            onBackButtonClicked(
                coroutineScope = coroutineScope,
                bottomSheetScaffoldState = bottomSheetScaffoldState,
                defaultAction = state.onBackButtonClick,
            )
        },
    )

    var floatingButtonHeight by remember { mutableStateOf(0.dp) }
    BottomSheetScaffold(
        modifier = modifier,
        sheetContent = {
            SheetContent(
                coroutineScope = coroutineScope,
                bottomSheetScaffoldState = bottomSheetScaffoldState,
                model = state.bottomSheet,
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        topBar = {
            AddCustomTokenToolbar(
                title = state.toolbar.title,
                onBackButtonClick = {
                    onBackButtonClicked(
                        coroutineScope,
                        bottomSheetScaffoldState,
                        defaultAction = state.toolbar.onBackButtonClick,
                    )
                },
            )
        },
        floatingActionButton = {
            val density = LocalDensity.current
            val verticalPadding = TangemTheme.dimens.spacing32
            AddCustomTokenFloatingButton(
                model = state.floatingButton,
                modifier = Modifier.onSizeChanged {
                    floatingButtonHeight = with(density) { it.height.toDp() + verticalPadding }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        sheetBackgroundColor = TangemTheme.colors.background.secondary,
        sheetPeekHeight = TangemTheme.dimens.size0,
        backgroundColor = TangemTheme.colors.background.secondary,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues = it)
                .padding(bottom = floatingButtonHeight)
                .fillMaxSize(),
        ) {
            TestBlock(
                state.testBlock,
                coroutineScope,
                bottomSheetScaffoldState,
            )

            AddCustomTokenForm(model = state.form)

            AddCustomTokenWarnings(warnings = state.warnings)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun onBackButtonClicked(
    coroutineScope: CoroutineScope,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    defaultAction: () -> Unit,
) {
    coroutineScope.launch {
        if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
            bottomSheetScaffoldState.bottomSheetState.collapse()
        } else {
            defaultAction()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SheetContent(
    model: AddCustomTokenChooseTokenBottomSheet,
    coroutineScope: CoroutineScope,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(LocalConfiguration.current.screenHeightDp.dp - TangemTheme.dimens.spacing16),
    ) {
        Hand()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            model.categoriesBlocks.forEachIndexed { index, categoryBlock ->
                key(categoryBlock) {
                    Column {
                        TokensList(
                            title = categoryBlock.name,
                            tokens = categoryBlock.items,
                            onTestTokenClick = { address ->
                                model.onTestTokenClick(address)
                                coroutineScope.launch { bottomSheetScaffoldState.bottomSheetState.collapse() }
                            },
                        )

                        if (model.categoriesBlocks.lastIndex != index) {
                            Divider()
                            SpacerH8()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokensList(title: String, tokens: List<TestTokenItem>, onTestTokenClick: (String) -> Unit) {
    Text(
        text = title,
        modifier = Modifier.padding(
            horizontal = TangemTheme.dimens.spacing24,
            vertical = TangemTheme.dimens.spacing8,
        ),
        maxLines = 1,
        style = TangemTheme.typography.h3,
    )

    tokens.forEach { token ->
        key(token) {
            PrimaryButton(
                text = token.name,
                onClick = { onTestTokenClick(token.address) },
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .padding(bottom = TangemTheme.dimens.spacing8)
                    .fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TestBlock(
    model: AddCustomTokenTestBlock,
    coroutineScope: CoroutineScope,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .padding(top = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
    ) {
        val softwareKeyboardController = LocalSoftwareKeyboardController.current
        PrimaryButton(
            text = model.chooseTokenButtonText,
            onClick = {
                softwareKeyboardController?.hide()
                coroutineScope.launch { bottomSheetScaffoldState.bottomSheetState.expand() }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(
                text = model.clearButtonText,
                onClick = model.onClearAddressButtonClick,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = model.resetButtonText,
                onClick = model.onResetButtonClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun Preview_AddCustomTokenTestContent() {
    TangemTheme {
        AddCustomTokenTestContent(state = AddCustomTokenPreviewData.createTestContent())
    }
}