package com.tangem.feature.walletsettings.utils

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.operations.attestation.ArtworkSize
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WalletCardItemDelegate @AssistedInject constructor(
    private val walletImageFetcher: UserWalletImageFetcher,
    @Assisted private val dialogNavigation: SlotNavigation<DialogConfig>,
    @Assisted private val onUpgradeHotWalletClick: () -> Unit,
) {

    fun cardItemFlow(wallet: UserWallet): Flow<WalletSettingsItemUM.CardBlock> {
        return walletImageFetcher.walletImage(wallet, ArtworkSize.SMALL).map { imageState ->
            val walletName = wallet.name
            WalletSettingsItemUM.CardBlock(
                id = "wallet_name",
                title = resourceReference(id = R.string.user_wallet_list_rename_popup_placeholder),
                text = stringReference(walletName),
                isEnabled = true,
                onClick = { openRenameWalletDialog(wallet) },
                imageState = imageState,
                additionalBlock = buildUpgradeToHardwareWalletBlockOrNull(wallet),
            )
        }
    }

    private fun buildUpgradeToHardwareWalletBlockOrNull(wallet: UserWallet): BlockUM? {
        return BlockUM(
            text = resourceReference(R.string.upgrade_to_hardware_wallet_button_title),
            iconRes = null,
            onClick = onUpgradeHotWalletClick,
            accentType = BlockUM.AccentType.ACCENT,
        ).takeIf { wallet is UserWallet.Hot }
    }

    private fun openRenameWalletDialog(userWallet: UserWallet) {
        val config = DialogConfig.RenameWallet(
            userWalletId = userWallet.walletId,
            currentName = userWallet.name,
        )
        dialogNavigation.activate(config)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            dialogNavigation: SlotNavigation<DialogConfig>,
            onUpgradeHotWalletClick: () -> Unit,
        ): WalletCardItemDelegate
    }
}