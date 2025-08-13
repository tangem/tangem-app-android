package com.tangem.features.managetokens.choosetoken.entity

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ChooseManageTokensBottomSheetConfig {

    @Serializable
    data class SwapTokensBottomSheetConfig(
        val userWalletId: UserWalletId,
        val initialCurrency: CryptoCurrency,
        val selectedCurrency: CryptoCurrency?,
        val token: ManagedCryptoCurrency.Token,
    ) : ChooseManageTokensBottomSheetConfig()
}