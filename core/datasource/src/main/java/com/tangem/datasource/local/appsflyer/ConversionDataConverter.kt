package com.tangem.datasource.local.appsflyer

import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.converter.TwoWayConverter

internal object ConversionDataConverter : TwoWayConverter<AppsFlyerConversionData, ConversionDataDTO> {

    override fun convert(value: AppsFlyerConversionData): ConversionDataDTO {
        return ConversionDataDTO(
            refcode = value.refcode,
            campaign = value.campaign,
        )
    }

    override fun convertBack(value: ConversionDataDTO): AppsFlyerConversionData {
        return AppsFlyerConversionData(
            refcode = value.refcode,
            campaign = value.campaign,
        )
    }
}