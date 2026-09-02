package com.tangem.data.collectibles

import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.model.MachineCategoryDTO
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import java.math.BigDecimal
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO as TierRangeDTO

internal object MachineFactory {

    val userWalletId = UserWalletId("011")

    fun create(
        id: String = "uid",
        externalId: String = "pokemon_50",
        price: BigDecimal = BigDecimal("50"),
        expectedValue: BigDecimal = BigDecimal("54.578583779326468"),
        categoryId: String = "pokemons",
        contains: Int? = 1,
        imageUrl: String? = "s3 url",
        legendaryOdds: Int? = 1,
        legendaryStock: Int? = 2628,
        legendaryRange: CollectibleGachaMachine.TierRange? =
            CollectibleGachaMachine.TierRange(start = 250, end = 5001),
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
        odds = CollectibleGachaMachine.Odds(common = 80, rare = 15, epic = 4, legendary = legendaryOdds),
        tierRanges = CollectibleGachaMachine.TierRanges(
            common = CollectibleGachaMachine.TierRange(start = 30, end = 60),
            rare = CollectibleGachaMachine.TierRange(start = 60, end = 110),
            epic = CollectibleGachaMachine.TierRange(start = 110, end = 250),
            legendary = legendaryRange,
        ),
        stock = CollectibleGachaMachine.Stock(common = 1949, rare = 3225, epic = 2375, legendary = legendaryStock),
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
        legendaryOdds: Int? = 1,
        legendaryStock: Int? = 2628,
        legendaryRange: TierRangeDTO? = TierRangeDTO(start = 250, end = 5001),
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
        odds = MachineDTO.MachineOddsDTO(common = 80, rare = 15, epic = 4, legendary = legendaryOdds),
        tierRanges = MachineDTO.MachineTierRangesDTO(
            common = TierRangeDTO(start = 30, end = 60),
            rare = TierRangeDTO(start = 60, end = 110),
            epic = TierRangeDTO(start = 110, end = 250),
            legendary = legendaryRange,
        ),
        stock = MachineDTO.MachineStockDTO(common = 1949, rare = 3225, epic = 2375, legendary = legendaryStock),
        expectedValue = expectedValue,
        image = image,
        categoryId = categoryId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun createListConfig(
        categoryId: String? = null,
        search: String? = null,
    ) = CollectibleGachaMachinesListConfig(categoryId = categoryId, search = search)

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