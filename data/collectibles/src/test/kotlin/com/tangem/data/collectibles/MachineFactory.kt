package com.tangem.data.collectibles

import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineNftsResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.model.MachineCategoryDTO
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import com.tangem.grow.datasource.collectibles.models.response.model.MachineNftDTO
import java.math.BigDecimal
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO.MachineTierRangesDTO.MachineTierRangeDTO as TierRangeDTO

/**
 * Fixtures for the collectibles data tests.
 *

 * does, in whole currency units. The two are related by `MachineConverter`, so they take different literals
 * for the same pack: `5000` on the dto, `50.00` on the model.
 */
internal object MachineFactory {

    val userWalletId = UserWalletId("011")

    fun create(
        id: String = "uid",
        externalId: String = "pokemon_50",
        price: BigDecimal = BigDecimal("50.00"),
        expectedValue: BigDecimal = BigDecimal("54.578583779326468"),
        categoryId: String = "pokemons",
        contains: Int? = 1,
        imageUrl: String? = "s3 url",
        epicOdds: Int? = 1,
        epicStock: Int? = 2628,
        epicRange: CollectibleGachaMachine.TierRange? =
            CollectibleGachaMachine.TierRange(start = 250, end = 5001),
    ) = CollectibleGachaMachine(
        id = id,
        externalId = externalId,
        name = "Elite Pokémon Gacha Pack",
        shortName = "PKMN 50",
        price = price,
        fee = BigDecimal("2.00"),
        feeAmount = BigDecimal("1.00"),
        contains = contains,
        instantBuybackPercent = 85,
        lowThreshold = 50,
        odds = CollectibleGachaMachine.Odds(common = 80, uncommon = 15, rare = 4, epic = epicOdds),
        tierRanges = CollectibleGachaMachine.TierRanges(
            common = CollectibleGachaMachine.TierRange(start = 30, end = 60),
            uncommon = CollectibleGachaMachine.TierRange(start = 60, end = 110),
            rare = CollectibleGachaMachine.TierRange(start = 110, end = 250),
            epic = epicRange,
        ),
        stock = CollectibleGachaMachine.Stock(common = 1949, uncommon = 3225, rare = 2375, epic = epicStock),
        expectedValue = expectedValue,
        imageUrl = imageUrl,
        categoryId = categoryId,
    )

    fun createDto(
        id: String = "uid",
        externalId: String = "pokemon_50",
        price: BigDecimal = BigDecimal("5000"),
        expectedValue: BigDecimal = BigDecimal("54.578583779326468"),
        categoryId: String = "pokemons",
        contains: Int? = 1,
        image: String? = "s3 url",
        createdAt: String = "2025-02-17 10:31:20.050 +0000",
        updatedAt: String = "2025-02-17 10:31:20.050 +0000",
        epicOdds: Int? = 1,
        epicStock: Int? = 2628,
        epicRange: TierRangeDTO? = TierRangeDTO(start = 250, end = 5001),
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
        odds = MachineDTO.MachineOddsDTO(common = 80, uncommon = 15, rare = 4, epic = epicOdds),
        tierRanges = MachineDTO.MachineTierRangesDTO(
            common = TierRangeDTO(start = 30, end = 60),
            uncommon = TierRangeDTO(start = 60, end = 110),
            rare = TierRangeDTO(start = 110, end = 250),
            epic = epicRange,
        ),
        stock = MachineDTO.MachineStockDTO(common = 1949, uncommon = 3225, rare = 2375, epic = epicStock),
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

    fun createNft(
        id: String = "nft_1",
        nftAddress: String = "GmnqDr9HsNd7o54YksNHrTQGGpFbY7w7aBugL3heUZhZ",
        name: String = "2024 #208 Raging Bolt EX PSA 10",
        description: String? = "2024 #208 Raging Bolt EX PSA 10 Tef EN-Temporal Forces Pokemon",
        rarity: CollectibleRarity? = CollectibleRarity.RARE,
        grade: CollectibleGachaMachineNft.Grade? = CollectibleGachaMachineNft.Grade(company = "PSA", value = "10"),
        imageUrl: String? = "https://cdn.tangem.com/nft.png",
        insuredValue: BigDecimal? = BigDecimal("2.40"),
    ) = CollectibleGachaMachineNft(
        id = id,
        nftAddress = nftAddress,
        name = name,
        description = description,
        rarity = rarity,
        grade = grade,
        imageUrl = imageUrl,
        insuredValue = insuredValue,
    )

    fun createNftDto(
        id: String = "nft_1",
        nftAddress: String = "GmnqDr9HsNd7o54YksNHrTQGGpFbY7w7aBugL3heUZhZ",
        name: String = "2024 #208 Raging Bolt EX PSA 10",
        description: String? = "2024 #208 Raging Bolt EX PSA 10 Tef EN-Temporal Forces Pokemon",
        rarity: String? = "rare",
        gradingCompany: String? = "PSA",
        gradeNum: String? = "10",
        image: String? = "https://cdn.tangem.com/nft.png",
        insuredValue: BigDecimal? = BigDecimal("240"),
    ) = MachineNftDTO(
        id = id,
        nftAddress = nftAddress,
        name = name,
        description = description,
        rarity = rarity,
        gradingCompany = gradingCompany,
        gradeNum = gradeNum,
        image = image,
        insuredValue = insuredValue,
    )

    fun createNftsResponse(
        nfts: List<MachineNftDTO> = listOf(createNftDto()),
        hasMore: Boolean = false,
        page: Int = 1,
        limit: Int = 50,
    ) = MachineNftsResponse(nfts = nfts, hasMore = hasMore, page = page, limit = limit)

    fun createNftsListConfig(externalId: String = "pokemon_50") =
        CollectibleGachaMachineNftsListConfig(externalId = externalId)
}