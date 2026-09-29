package com.tangem.data.collectibles.converter

import com.tangem.data.collectibles.CollectiblesConst.CENTS_SCALE
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import com.tangem.utils.converter.Converter

internal object MachineConverter : Converter<MachineDTO, CollectibleGachaMachine> {

    override fun convert(value: MachineDTO): CollectibleGachaMachine = CollectibleGachaMachine(
        id = value.id,
        externalId = value.externalId,
        name = value.name,
        shortName = value.shortName,
        price = value.price.movePointLeft(CENTS_SCALE),
        fee = value.fee.movePointLeft(CENTS_SCALE),
        feeAmount = value.feeAmount.movePointLeft(CENTS_SCALE),
        contains = value.contains,
        instantBuybackPercent = value.instantBuybackPercent,
        lowThreshold = value.lowThreshold,
        odds = value.odds.toDomain(),
        tierRanges = value.tierRanges.toDomain(),
        stock = value.stock.toDomain(),
        expectedValue = value.expectedValue.movePointLeft(CENTS_SCALE),
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

    private fun MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO.toDomain() = CollectibleGachaMachine.TierRange(
        start = start.movePointLeft(CENTS_SCALE),
        end = end.movePointLeft(CENTS_SCALE),
    )
}