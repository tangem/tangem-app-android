package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TangemPayCardPageUM(
    val addToWalletBlockState: AddToWalletBlockState? = null,
    val settings: ImmutableList<TangemPayCardPageSetting> = persistentListOf(
        TangemPayCardPageSetting.ChangePIN,
        TangemPayCardPageSetting.FreezeCard,
    ),
    val onBackClick: () -> Unit,
    val onSettingClick: (TangemPayCardPageSetting) -> Unit,
) {
    companion object {
        fun stub(
            addToWalletBlockState: AddToWalletBlockState? = AddToWalletBlockState(onClick = {}, onClickClose = {}),
            settings: ImmutableList<TangemPayCardPageSetting> = persistentListOf(
                TangemPayCardPageSetting.ChangePIN,
                TangemPayCardPageSetting.FreezeCard,
                TangemPayCardPageSetting.ReplaceCard,
            ),
        ) = TangemPayCardPageUM(
            addToWalletBlockState = addToWalletBlockState,
            settings = settings,
            onBackClick = {},
            onSettingClick = {},
        )
    }
}

@Immutable
internal sealed class TangemPayCardPageSetting {
    data object ChangePIN : TangemPayCardPageSetting()
    data object FreezeCard : TangemPayCardPageSetting()
    data object ReplaceCard : TangemPayCardPageSetting()
}