package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import com.tangem.utils.converter.Converter

internal object MachineConverter : Converter<MachineDTO, CollectibleGachaMachine> {

    override fun convert(value: MachineDTO): CollectibleGachaMachine = CollectibleGachaMachine(
        id = value.id,
        externalId = value.externalId,
        name = value.name,
        shortName = value.shortName,
        price = value.price.movePointLeft(2),
        fee = value.fee.movePointLeft(2),
        feeAmount = value.feeAmount.movePointLeft(2),
        contains = value.contains,
        instantBuybackPercent = value.instantBuybackPercent,
        lowThreshold = value.lowThreshold,
        odds = value.odds.toDomain(),
        tierRanges = value.tierRanges.toDomain(),
        stock = value.stock.toDomain(),
        expectedValue = value.expectedValue,
        imageUrl = value.image,
        categoryId = value.categoryId,
    )

    private fun MachineDTO.MachineOddsDTO.toDomain() = CollectibleGachaMachine.Odds(
        common = common,
        uncommon = uncommon,
        rare = rare,
        epic = epic,
    )

    private fun MachineDTO.MachineStockDTO.toDomain() = CollectibleGachaMachine.Stock(
        common = common,
        uncommon = uncommon,
        rare = rare,
        epic = epic,
    )

    private fun MachineDTO.MachineTierRangesDTO.toDomain() = CollectibleGachaMachine.TierRanges(
        common = common.toDomain(),
        uncommon = uncommon.toDomain(),
        rare = rare.toDomain(),
        epic = epic?.toDomain(),
    )

    private fun MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO.toDomain() =
        CollectibleGachaMachine.TierRange(start = start, end = end)
}