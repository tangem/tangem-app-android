package com.tangem.features.collectibles.impl.addfunds

import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWalletId

internal object CollectiblesReceiveConfigFactory {

    fun create(vault: CollectiblesVaultBalance, userWalletId: UserWalletId) = TokenReceiveConfig(
        shouldShowWarning = true,
        cryptoCurrency = vault.usdcToken,
        userWalletId = userWalletId,
        shouldShowMemoDisclaimer = false,
        receiveAddress = listOf(
            ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Default, value = vault.vaultAddress),
        ),
    )
}