package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder

/**
 * Add custom token screen
 *
 * @param stateHolder state holder
 *
 * @author Andrew Khokhlov on 18/04/2023
 */
@Composable
internal fun AddCustomTokenScreen(stateHolder: AddCustomTokenStateHolder, modifier: Modifier = Modifier) {
    when (stateHolder) {
        is AddCustomTokenStateHolder.Content -> AddCustomTokenContent(stateHolder, modifier)
        is AddCustomTokenStateHolder.TestContent -> AddCustomTokenTestContent(stateHolder, modifier)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Preview_AddCustomTokenScreen_Light(
    @PreviewParameter(AddCustomTokenScreenProvider::class) stateHolder: AddCustomTokenStateHolder,
) {
    TangemTheme(isDark = false) {
        AddCustomTokenScreen(stateHolder)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Preview_AddCustomTokenScreen_Dark(
    @PreviewParameter(AddCustomTokenScreenProvider::class) stateHolder: AddCustomTokenStateHolder,
) {
    TangemTheme(isDark = true) {
        AddCustomTokenScreen(stateHolder)
    }
}

private class AddCustomTokenScreenProvider : CollectionPreviewParameterProvider<AddCustomTokenStateHolder>(
    collection = listOf(
        AddCustomTokenPreviewData.createContent(),
        AddCustomTokenPreviewData.createTestContent(),
    ),
)
