package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency

/**
 * Whether a Prediction account belongs to this wallet at all. Unlike
 * [com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase] it ignores the lock: the account is
 * part of the wallet's structure and must not come and go with it.
 */
val UserWallet.isPredictionAccountSupported: Boolean
    get() = isMultiCurrency