package com.tangem.feature.walletsettings.utils

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsSyncUseCase
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.operations.attestation.ArtworkSize
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

internal class WalletCardItemDelegate @AssistedInject constructor(
    private val getShouldSaveUserWalletsSyncUseCase: ShouldSaveUserWalletsSyncUseCase,
    private val walletImageFetcher: UserWalletImageFetcher,
    @Assisted private val dialogNavigation: SlotNavigation<DialogConfig>,
) {

    fun cardItemFlow(wallet: UserWallet): Flow<WalletSettingsItemUM.CardBlock> = combine(
        flow = walletImageFetcher.walletImage(wallet, ArtworkSize.SMALL),
        flow2 = flow { emit(getShouldSaveUserWalletsSyncUseCase()) },
        transform = { imageState, isRenameAvailable ->
            val walletName = wallet.name
            WalletSettingsItemUM.CardBlock(
                id = "wallet_name",
                title = resourceReference(id = R.string.user_wallet_list_rename_popup_placeholder),
                text = stringReference(walletName),
                isEnabled = isRenameAvailable,
                onClick = { openRenameWalletDialog(wallet) },
                imageState = imageState,
            )
        },
    )

    private fun openRenameWalletDialog(userWallet: UserWallet) {
        val config = DialogConfig.RenameWallet(
            userWalletId = userWallet.walletId,
            currentName = userWallet.name,
        )
        dialogNavigation.activate(config)
    }

    @AssistedFactory
    interface Factory {
        fun create(dialogNavigation: SlotNavigation<DialogConfig>): WalletCardItemDelegate
    }
}