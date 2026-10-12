package com.tangem.features.collectibles.impl.addfunds

import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWalletId

// TODO([REDACTED_TASK_KEY]): clarify which address belongs here — the vault's USDC ATA is what is shown now, and it is
//  unconfirmed that an external sender or an exchange can pay to it.
internal object CollectiblesReceiveConfigFactory {

    fun create(vault: CollectiblesVaultBalance, userWalletId: UserWalletId) = TokenReceiveConfig(
        shouldShowWarning = true,
        cryptoCurrency = vault.usdcToken,
        userWalletId = userWalletId,
        showMemoDisclaimer = false,
        receiveAddress = listOf(
            ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Default, value = vault.ataAddress),
        ),
    )
}