package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TangemPayCardPageUM(
    val settings: ImmutableList<TangemPayCardPageSetting>,
    val onBackClick: () -> Unit,
    val addToWalletBlockState: AddToWalletBlockState? = null,
    val isReissueInProgress: Boolean = false,
) {
    companion object {
        fun stub(
            addToWalletBlockState: AddToWalletBlockState? = AddToWalletBlockState(onClick = {}, onClickClose = {}),
            settings: ImmutableList<TangemPayCardPageSetting> = persistentListOf(
                TangemPayCardPageSetting(TextReference.Str("Pin Code")) {},
                TangemPayCardPageSetting(TextReference.Str("Freeze Card")) {},
                TangemPayCardPageSetting(TextReference.Str("Reissue Card")) {},
            ),
            isReissueInProgress: Boolean = false,
        ) = TangemPayCardPageUM(
            addToWalletBlockState = addToWalletBlockState,
            settings = settings,
            onBackClick = {},
            isReissueInProgress = isReissueInProgress,
        )
    }
}

@Immutable
internal data class TangemPayCardPageSetting(
    val title: TextReference,
    val onSettingClick: () -> Unit,
)