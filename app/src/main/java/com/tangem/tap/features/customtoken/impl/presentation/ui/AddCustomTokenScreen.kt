package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.compose.runtime.Composable
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
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenScreen(stateHolder: AddCustomTokenStateHolder) {
    when (stateHolder) {
        is AddCustomTokenStateHolder.Content -> AddCustomTokenContent(state = stateHolder)
        is AddCustomTokenStateHolder.TestContent -> AddCustomTokenTestContent(state = stateHolder)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Preview_AddCustomTokenScreen(
    @PreviewParameter(AddCustomTokenScreenProvider::class) stateHolder: AddCustomTokenStateHolder,
) {
    TangemTheme {
        AddCustomTokenScreen(stateHolder)
    }
}

private class AddCustomTokenScreenProvider : CollectionPreviewParameterProvider<AddCustomTokenStateHolder>(
    collection = listOf(
        AddCustomTokenPreviewData.createContent(),
        AddCustomTokenPreviewData.createTestContent(),
    ),
)