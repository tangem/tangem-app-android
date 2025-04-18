package com.tangem.features.nft.details.info

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.details.info.ui.NFTInfoBottomSheet
import com.tangem.features.nft.details.info.ui.NFTInfoBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTDetailsInfoComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTDetailsInfoComponent.Params,
) : NFTDetailsInfoComponent, AppComponentContext by context {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }
        NFTInfoBottomSheet(
            config = bottomSheetConfig,
            content = {
                NFTInfoBottomSheetContent(
                    text = params.text,
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing16),
                )
            },
        )
    }

    @AssistedFactory
    interface Factory : NFTDetailsInfoComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTDetailsInfoComponent.Params,
        ): DefaultNFTDetailsInfoComponent
    }
}