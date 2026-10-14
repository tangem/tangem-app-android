package com.tangem.features.collectibles.impl.machinedetails.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.features.collectibles.impl.common.formatter.formatUsd
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.utils.converter.Converter

internal object CollectiblesPrizeConverter :
    Converter<CollectibleGachaMachineNft, CollectiblesMachineDetailsUM.PrizeUM> {

    override fun convert(value: CollectibleGachaMachineNft): CollectiblesMachineDetailsUM.PrizeUM {
        return CollectiblesMachineDetailsUM.PrizeUM(
            id = value.id,
            imageUrl = value.frontImageUrl,
            grade = value.grade?.let { stringReference("${it.company}${it.value}") },
            title = stringReference(value.name),
            value = stringReference(value.insuredValue.formatUsd()),
        )
    }
}