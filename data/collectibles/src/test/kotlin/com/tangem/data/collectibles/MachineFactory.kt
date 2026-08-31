package com.tangem.data.collectibles

import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.model.MachineCategoryDTO
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import java.math.BigDecimal

internal object MachineFactory {

    fun create(
        id: String = "uid",
        externalId: String = "pokemon_50",
        price: BigDecimal = BigDecimal("50"),
        expectedValue: BigDecimal = BigDecimal("54.578583779326468"),
        categoryId: String = "pokemons",
        contains: Int? = 1,
        imageUrl: String? = "s3 url",
    ) = CollectibleGachaMachine(
        id = id,
        externalId = externalId,
        name = "Elite Pokémon Gacha Pack",
        shortName = "PKMN 50",
        price = price,
        fee = BigDecimal("200"),
        feeAmount = BigDecimal("100"),
        contains = contains,
        instantBuybackPercent = 85,
        lowThreshold = 50,
        odds = CollectibleGachaMachine.Odds(common = 80, uncommon = 15, rare = 4, epic = 1),
        tierRanges = CollectibleGachaMachine.TierRanges(
            common = CollectibleGachaMachine.TierRange(start = 30, end = 60),
            uncommon = CollectibleGachaMachine.TierRange(start = 60, end = 110),
            rare = CollectibleGachaMachine.TierRange(start = 110, end = 250),
            epic = CollectibleGachaMachine.TierRange(start = 250, end = 5001),
        ),
        stock = CollectibleGachaMachine.Stock(common = 1949, uncommon = 3225, rare = 2375, epic = 2628),
        expectedValue = expectedValue,
        imageUrl = imageUrl,
        categoryId = categoryId,
    )

    fun createDto(
        id: String = "uid",
        externalId: String = "pokemon_50",
        price: BigDecimal = BigDecimal("50"),
        expectedValue: BigDecimal = BigDecimal("54.578583779326468"),
        categoryId: String = "pokemons",
        contains: Int? = 1,
        image: String? = "s3 url",
        createdAt: String = "2025-02-17 10:31:20.050 +0000",
        updatedAt: String = "2025-02-17 10:31:20.050 +0000",
    ) = MachineDTO(
        id = id,
        externalId = externalId,
        name = "Elite Pokémon Gacha Pack",
        shortName = "PKMN 50",
        price = price,
        fee = BigDecimal("200"),
        feeAmount = BigDecimal("100"),
        contains = contains,
        instantBuybackPercent = 85,
        lowThreshold = 50,
        odds = MachineDTO.MachineOddsDTO(common = 80, uncommon = 15, rare = 4, epic = 1),
        tierRanges = MachineDTO.MachineTierRangesDTO(
            common = MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO(start = 30, end = 60),
            uncommon = MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO(start = 60, end = 110),
            rare = MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO(start = 110, end = 250),
            epic = MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO(start = 250, end = 5001),
        ),
        stock = MachineDTO.MachineStockDTO(common = 1949, uncommon = 3225, rare = 2375, epic = 2628),
        expectedValue = expectedValue,
        image = image,
        categoryId = categoryId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun createCategoryDto(
        id: String = "pokemons",
        name: String = "Pokemons",
        machinesQuantity: Int = 10,
    ) = MachineCategoryDTO(id = id, name = name, machinesQuantity = machinesQuantity)

    fun createCategoriesResponse(
        categories: List<MachineCategoryDTO> = listOf(createCategoryDto()),
        totalMachinesQuantity: Int = 47,
    ) = MachineCategoriesResponse(categories = categories, totalMachinesQuantity = totalMachinesQuantity)

    fun createMachinesResponse(
        machines: List<MachineDTO> = listOf(createDto()),
        hasNext: Boolean = false,
        cursor: String? = null,
    ) = MachinesResponse(machines = machines, hasNext = hasNext, cursor = cursor)
}