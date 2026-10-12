package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.grow.datasource.collectibles.models.request.GenerateTxRequestBody
import com.tangem.utils.converter.TwoWayConverter

internal object TurboModeConverter : TwoWayConverter<GenerateTxRequestBody.TurboModeDTO, CollectibleTurboMode> {
    override fun convert(value: GenerateTxRequestBody.TurboModeDTO): CollectibleTurboMode {
        return CollectibleTurboMode(isEnabled = value.isEnabled, buybackAddress = value.buybackAddress)
    }

    override fun convertBack(value: CollectibleTurboMode): GenerateTxRequestBody.TurboModeDTO {
        return GenerateTxRequestBody.TurboModeDTO(isEnabled = value.isEnabled, buybackAddress = value.buybackAddress)
    }
}