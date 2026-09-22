package com.tangem.domain.collectibles

import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

internal object CollectiblesModelFactory {

    val userWalletId = UserWalletId("011")

    fun createMachine(
        id: String = "uid",
        externalId: String = "pokemon_50",
        categoryId: String = "pokemons",
    ) = CollectibleGachaMachine(
        id = id,
        externalId = externalId,
        name = "Elite Pokémon Gacha Pack",
        shortName = "PKMN 50",
        price = BigDecimal("50"),
        fee = BigDecimal("200"),
        feeAmount = BigDecimal("100"),
        contains = 1,
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
        expectedValue = BigDecimal("54.578583779326468"),
        imageUrl = "s3 url",
        categoryId = categoryId,
    )

    fun createCategories(
        categories: List<CollectibleGachaMachineCategories.MachineCategory> = listOf(createCategory()),
        totalMachinesQuantity: Int = 47,
    ) = CollectibleGachaMachineCategories(
        categories = categories,
        totalMachinesQuantity = totalMachinesQuantity,
    )

    fun createCategory(
        id: String = "pokemons",
        name: String = "Pokemons",
        machinesQuantity: Int = 10,
    ) = CollectibleGachaMachineCategories.MachineCategory(
        id = id,
        name = name,
        machinesQuantity = machinesQuantity,
    )
}