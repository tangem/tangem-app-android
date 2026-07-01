package com.tangem.data.common.converter

import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.utils.converter.Converter

/**
 * Maps a persisted [ExpressProviderEntity] into the domain [ExpressProvider].
 */
class ExpressProviderConverter : Converter<ExpressProviderEntity, ExpressProvider> {

    override fun convert(value: ExpressProviderEntity): ExpressProvider {
        return ExpressProvider(
            providerId = value.id,
            rateTypes = emptyList(),
            name = value.name,
            type = value.type.toExpressProviderType(),
            imageLarge = value.imageLarge,
            termsOfUse = value.termsOfUse,
            privacyPolicy = value.privacyPolicy,
            isRecommended = value.isRecommended,
            slippage = value.slippage?.toBigDecimalOrNull(),
            isExchangeOnlyWithinSingleAddress = value.isExchangeOnlyWithinSingleAddress,
            isExtraIdSupported = value.isExtraIdSupported,
        )
    }

    private fun String.toExpressProviderType(): ExpressProviderType = ExpressProviderType.valueOf(this)
}