package com.tangem.domain.wallets

enum class PromoCodeActivationResult {
    Failed,
    InvalidPromoCode,
    NoBitcoinAddress,
    PromoCodeAlreadyUsed,
    Activated,
}