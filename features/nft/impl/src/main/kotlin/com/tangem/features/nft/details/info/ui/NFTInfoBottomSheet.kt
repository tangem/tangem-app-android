package com.tangem.features.nft.details.info.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun NFTInfoBottomSheet(
    title: TextReference,
    config: TangemBottomSheetConfig,
    content: @Composable () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        titleText = title,
        content = { content() },
    )
}

@Composable
internal fun NFTInfoBottomSheetContent(text: TextReference, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.verticalScroll(scrollState),
    ) {
        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.body2,
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_NFTInfoBottomSheetContent() {
    TangemThemePreview {
        NFTInfoBottomSheetContent(
            text = stringReference(
                """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce varius neque vel ligula 
                    tincidunt, nec faucibus nulla ultricies. Maecenas euismod arcu in nunc volutpat, 
                    at bibendum eros lacinia. Proin hendrerit massa non velit congue, 
                    in volutpat nisi consequat. Sed vitae justo nec orci tincidunt malesuada. 
                    Nullam feugiat purus vel lectus efficitur, vel fringilla urna volutpat. 
                    Donec sagittis enim in metus lacinia, vel tempor nunc bibendum. 
                """.trimIndent(),
            ),
        )
    }
}