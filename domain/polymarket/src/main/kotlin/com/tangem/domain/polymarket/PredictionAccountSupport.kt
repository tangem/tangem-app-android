package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency

/**
 * Whether a Prediction account belongs to this wallet at all.
 *
 * Says nothing about the wallet being locked, unlike
 * [com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase], which shares this rule and adds
 * that one: onboarding needs an unlocked wallet to derive the owner address, while the account is part of the
 * wallet's structure and must not appear and disappear as the wallet is locked and unlocked.
 */
val UserWallet.isPredictionAccountSupported: Boolean
    get() = isMultiCurrency