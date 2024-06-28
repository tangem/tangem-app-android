package com.tangem.tap.features.customtoken.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder

/**
 * Add custom token screen
 *
 * @param stateHolder state holder
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenScreen(stateHolder: AddCustomTokenStateHolder, modifier: Modifier = Modifier) {
    when (stateHolder) {
        is AddCustomTokenStateHolder.Content -> AddCustomTokenContent(stateHolder, modifier)
        is AddCustomTokenStateHolder.TestContent -> AddCustomTokenTestContent(stateHolder, modifier)
    }
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AddCustomTokenScreen(
    @PreviewParameter(AddCustomTokenScreenProvider::class) stateHolder: AddCustomTokenStateHolder,
) {
    TangemThemePreview {
        AddCustomTokenScreen(stateHolder)
    }
}

private class AddCustomTokenScreenProvider : CollectionPreviewParameterProvider<AddCustomTokenStateHolder>(
    collection = listOf(
        AddCustomTokenPreviewData.createContent(),
        AddCustomTokenPreviewData.createTestContent(),
    ),
)