package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.grow.datasource.collectibles.models.request.GeneratePurchaseTxRequestBody
import com.tangem.utils.converter.TwoWayConverter

internal object TurboModeConverter :
    TwoWayConverter<GeneratePurchaseTxRequestBody.TurboModeDTO, CollectibleTurboMode> {

    override fun convert(value: GeneratePurchaseTxRequestBody.TurboModeDTO): CollectibleTurboMode {
        return CollectibleTurboMode(isEnabled = value.isEnabled, buybackAddress = value.buybackAddress)
    }

    override fun convertBack(value: CollectibleTurboMode): GeneratePurchaseTxRequestBody.TurboModeDTO {
        return GeneratePurchaseTxRequestBody.TurboModeDTO(
            isEnabled = value.isEnabled,
            buybackAddress = value.buybackAddress,
        )
    }
}