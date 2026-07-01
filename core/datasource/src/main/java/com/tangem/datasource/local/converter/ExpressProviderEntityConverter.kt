package com.tangem.datasource.local.converter

import com.tangem.datasource.api.express.models.response.ExchangeProvider
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity

/**
 * Maps an [ExchangeProvider] API response into its persisted [ExpressProviderEntity] representation.
 *
 * `type` is stored as the [com.tangem.datasource.api.express.models.response.ExchangeProviderType] name
 * (DEX / CEX / DEX_BRIDGE / ONRAMP); `slippage` as a plain decimal string.
 */
fun ExchangeProvider.toEntity(): ExpressProviderEntity {
    return ExpressProviderEntity(
        id = id,
        name = name,
        type = type.name,
        imageLarge = imageLargeUrl,
        imageSmall = imageSmallUrl,
        termsOfUse = termsOfUse,
        privacyPolicy = privacyPolicy,
        isRecommended = isRecommended,
        slippage = slippage?.toPlainString(),
        isExchangeOnlyWithinSingleAddress = isExchangeOnlyWithinSingleAddress,
        isExtraIdSupported = isExtraIdSupported,
    )
}