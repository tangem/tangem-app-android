package com.tangem.features.nft.details.ui.bottomsheet

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheetTitle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.details.entity.NFTInfoBottomSheetConfig

@Composable
fun NFTInfoBottomSheet(config: TangemBottomSheetConfig) {
    val scrollState = rememberScrollState()

    TangemBottomSheet<NFTInfoBottomSheetConfig>(
        config = config,
        title = { content ->
            TangemBottomSheetTitle(title = content.title)
        },
    ) { content ->
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Text(
                text = content.text.resolveReference(),
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body2,
                modifier = Modifier.padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing16,
                ),
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_StakingInfoBottomSheet() {
    TangemThemePreview {
        NFTInfoBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = NFTInfoBottomSheetConfig(
                    title = stringReference("Title"),
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
                ),
            ),
        )
    }
}
