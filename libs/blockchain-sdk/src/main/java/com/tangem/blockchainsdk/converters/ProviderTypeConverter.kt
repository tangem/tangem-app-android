package com.tangem.blockchainsdk.converters

import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.providers.ProviderTypeIdMapping
import com.tangem.datasource.local.config.providers.models.ProviderModel
import com.tangem.utils.converter.TwoWayConverter

/**
 * Converts [ProviderModel] to [ProviderType] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object ProviderTypeConverter : TwoWayConverter<ProviderModel, ProviderType?> {

    override fun convert(value: ProviderModel): ProviderType? {
        return when (value) {
            is ProviderModel.Public -> ProviderType.Public(url = value.url)
            is ProviderModel.Private -> ProviderTypeIdMapping.entries.firstOrNull { it.id == value.name }?.providerType
            ProviderModel.UnsupportedType -> null
        }
    }

    override fun convertBack(value: ProviderType?): ProviderModel {
        return when (value) {
            null -> ProviderModel.UnsupportedType
            is ProviderType.Public -> ProviderModel.Public(url = value.url)
            else -> {
                val id = ProviderTypeIdMapping.entries.firstOrNull { it.providerType == value }?.id
                    ?: return ProviderModel.UnsupportedType

                ProviderModel.Private(id)
            }
        }
    }
}