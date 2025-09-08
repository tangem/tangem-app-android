package com.tangem.features.yieldlending.impl.promo.entity

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal data class StartEarningBottomSheetConfig(
    val userWalletId: UserWalletId,
    val cryptoCurrency: CryptoCurrency,
)