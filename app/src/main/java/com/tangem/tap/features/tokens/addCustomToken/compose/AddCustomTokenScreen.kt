package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.common.form.DataField
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.features.addCustomToken.redux.ScreenState
import com.tangem.domain.features.addCustomToken.redux.ViewStates
import com.tangem.domain.redux.domainStore
import com.tangem.tap.common.compose.*
import com.tangem.tap.common.moduleMessage.ModuleMessageConverter
import com.tangem.tap.features.tokens.addCustomToken.compose.test.TestCase
import com.tangem.tap.features.tokens.addCustomToken.compose.test.TestCasesList
import com.tangem.wallet.R
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 23/03/2022.
 */
private class AddCustomTokenScreen {} // for simple search

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddCustomTokenScreen(
    state: MutableState<AddCustomTokenState>,
    closePopupTrigger: ClosePopupTrigger,
) {
    val selectedTestCase = remember { mutableStateOf(TestCase.ContractAddress) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )
    val coroutineScope = rememberCoroutineScope()
    val toggleBottomSheet = { coroutineScope.launch { bottomSheetScaffoldState.toggle() } }

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = {
            Surface(color = colorResource(id = R.color.lightGray5)) {
                selectedTestCase.value.content(toggleBottomSheet)
            }
        },
        sheetPeekHeight = 0.dp,
    ) {
        Column() {
            TestCasesList(onItemClick = {
                selectedTestCase.value = it
                toggleBottomSheet()
            })
            ScreenContent(state, closePopupTrigger)
        }
    }

    ComposeDialogManager()
    LaunchedEffect(key1 = Unit, block = { domainStore.dispatch(AddCustomTokenAction.OnCreate) })
    DisposableEffect(key1 = Unit, effect = { onDispose { domainStore.dispatch(AddCustomTokenAction.OnDestroy) } })
}

@OptIn(ExperimentalMaterialApi::class)
private suspend fun BottomSheetScaffoldState.toggle() {
    if (bottomSheetState.isCollapsed) {
        bottomSheetState.expand()
    } else {
        bottomSheetState.collapse()
    }
}

@Composable
private fun ScreenContent(
    state: MutableState<AddCustomTokenState>,
    closePopupTrigger: ClosePopupTrigger,
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = colorResource(id = R.color.backgroundLightGray),
        floatingActionButton = {
            HangingOverKeyboardView(keyboardState = keyboardAsState()) {
                AddButton(state)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = MaterialTheme.shapes.small,
                        elevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            FormFields(state, closePopupTrigger)
                        }
                    }
                }
                item { Warnings(state.value.warnings.toList()) }
            }
        }
    }
}

@Composable
private fun FormFields(
    state: MutableState<AddCustomTokenState>,
    closePopupTrigger: ClosePopupTrigger,
) {
    val context = LocalContext.current
    val errorConverter = remember { ModuleMessageConverter(context) }

    val stateValue = state.value
    stateValue.form.fieldList.forEach { field ->
        val data = ScreenFieldData.fromState(field, stateValue, errorConverter)
        when (field.id) {
            ContractAddress -> TokenContractAddressView(data)
            Network -> TokenNetworkView(data, stateValue, closePopupTrigger)
            Name -> TokenNameView(data)
            Symbol -> TokenSymbolView(data)
            Decimals -> TokenDecimalsView(data)
            DerivationPath -> TokenDerivationPathView(data, stateValue, closePopupTrigger)
        }
    }
}

@Composable
fun Warnings(warnings: List<AddCustomTokenError.Warning>) {
    if (warnings.isEmpty()) return

    val context = LocalContext.current
    val warningConverter = remember { ModuleMessageConverter(context) }

    Column {
        warnings.forEachIndexed { index, item ->
            val modifier = when (index) {
                0 -> Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp)
                warnings.lastIndex -> Modifier.padding(16.dp, 8.dp, 16.dp, 16.dp)
                else -> Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp)
            }
            AddCustomTokenWarning(
                modifier = modifier.fillMaxWidth(),
                warning = item,
                converter = warningConverter
            )
        }
    }
}

@Composable
private fun AddButton(state: MutableState<AddCustomTokenState>) {
    AddCustomTokenFab(
        modifier = Modifier
            .widthIn(210.dp, 280.dp),
        isEnabled = state.value.screenState.addButton.isEnabled
    ) { domainStore.dispatch(AddCustomTokenAction.OnAddCustomTokenClicked) }
}

@Composable
private fun AddCustomTokenFab(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = Color.White
    val backgroundColor = if (isEnabled) {
        Color(0xFF1ACE80)
    } else {
        Color(0xFFB9E6D3)
    }
    val elevation = if (!isEnabled) {
        FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
    } else {
        FloatingActionButtonDefaults.elevation()
    }

    ToggledRippleTheme(isEnabled) {
        ExtendedFloatingActionButton(
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    tint = contentColor,
                    contentDescription = "Add",
                )
            },
            text = { Text(text = stringResource(id = R.string.common_add)) },
            onClick = { if (isEnabled) onClick() },
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            elevation = elevation,
        )
    }
}

data class ScreenFieldData(
    val field: DataField<*>,
    val error: AddCustomTokenError?,
    val errorConverter: ModuleMessageConverter,
    val viewState: ViewStates.TokenField
) {
    companion object {
        fun fromState(
            field: DataField<*>,
            state: AddCustomTokenState,
            errorConverter: ModuleMessageConverter
        ): ScreenFieldData {
            return ScreenFieldData(
                field = field,
                error = state.getError(field.id),
                errorConverter = errorConverter,
                viewState = selectField(field.id, state.screenState)
            )
        }

        private fun selectField(id: FieldId, screenState: ScreenState): ViewStates.TokenField {
            return when (id) {
                ContractAddress -> screenState.contractAddressField
                Network -> screenState.network
                Name -> screenState.name
                Symbol -> screenState.symbol
                Decimals -> screenState.decimals
                DerivationPath -> screenState.derivationPath
                else -> throw UnsupportedOperationException()
            }
        }
    }
}