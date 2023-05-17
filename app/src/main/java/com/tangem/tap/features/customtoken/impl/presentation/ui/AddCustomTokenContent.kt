package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenFloatingButton
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenToolbar
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenWarnings

/**
 * Add custom token content
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenContent(state: AddCustomTokenStateHolder.Content) {
    BackHandler(onBack = state.onBackButtonClick)

    var floatingButtonHeight by remember { mutableStateOf(0.dp) }
    Scaffold(
        topBar = {
            AddCustomTokenToolbar(
                title = state.toolbar.title,
                onBackButtonClick = state.toolbar.onBackButtonClick,
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
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues = it)
                .padding(bottom = floatingButtonHeight)
                .fillMaxSize(),
        ) {
            AddCustomTokenForm(model = state.form)

            AddCustomTokenWarnings(warnings = state.warnings)
        }
    }
}

@Preview
@Composable
private fun Preview_AddCustomTokenContent() {
    TangemTheme {
        AddCustomTokenContent(state = AddCustomTokenPreviewData.createContent())
    }
}