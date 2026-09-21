package com.tangem.features.collectibles.impl

import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchListState
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchListState
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import java.math.BigDecimal

/**
 * Fixtures for the discovery tests. Every argument is defaulted, so a test overrides only the fields it is
 * about — [CollectibleGachaMachine] alone has sixteen of them.
 */
internal fun createMachine(
    id: String = "machine_1",
    externalId: String = "pokemon_1",
    name: String = "Pokemon Legendary Collection",
    shortName: String = "Pokemon",
    price: BigDecimal = BigDecimal("10.00"),
    fee: BigDecimal = BigDecimal("0.05"),
    feeAmount: BigDecimal = BigDecimal("0.50"),
    contains: Int? = 3,
    instantBuybackPercent: Int = 0,
    lowThreshold: Int = 5,
    odds: CollectibleGachaMachine.Odds = createOdds(),
    tierRanges: CollectibleGachaMachine.TierRanges = createTierRanges(),
    stock: CollectibleGachaMachine.Stock = createStock(),
    expectedValue: BigDecimal = BigDecimal("12.00"),
    imageUrl: String? = "https://tangem.com/pack.png",
    categoryId: String = "sports",
): CollectibleGachaMachine = CollectibleGachaMachine(
    id = id,
    externalId = externalId,
    name = name,
    shortName = shortName,
    price = price,
    fee = fee,
    feeAmount = feeAmount,
    contains = contains,
    instantBuybackPercent = instantBuybackPercent,
    lowThreshold = lowThreshold,
    odds = odds,
    tierRanges = tierRanges,
    stock = stock,
    expectedValue = expectedValue,
    imageUrl = imageUrl,
    categoryId = categoryId,
)

internal fun createOdds(
    common: Int = 70,
    uncommon: Int = 20,
    rare: Int = 9,
    epic: Int? = 1,
): CollectibleGachaMachine.Odds = CollectibleGachaMachine.Odds(
    common = common,
    uncommon = uncommon,
    rare = rare,
    epic = epic,
)

internal fun createStock(
    common: Int = 100,
    uncommon: Int = 50,
    rare: Int = 10,
    epic: Int? = 1,
): CollectibleGachaMachine.Stock = CollectibleGachaMachine.Stock(
    common = common,
    uncommon = uncommon,
    rare = rare,
    epic = epic,
)

internal fun createTierRanges(
    common: CollectibleGachaMachine.TierRange = CollectibleGachaMachine.TierRange(start = 1, end = 10),
    uncommon: CollectibleGachaMachine.TierRange = CollectibleGachaMachine.TierRange(start = 11, end = 50),
    rare: CollectibleGachaMachine.TierRange = CollectibleGachaMachine.TierRange(start = 51, end = 200),
    epic: CollectibleGachaMachine.TierRange? = CollectibleGachaMachine.TierRange(start = 201, end = 1000),
): CollectibleGachaMachine.TierRanges = CollectibleGachaMachine.TierRanges(
    common = common,
    uncommon = uncommon,
    rare = rare,
    epic = epic,
)

internal fun createCategory(
    id: String = "sports",
    name: String = "Sports",
    machinesQuantity: Int = 12,
): CollectibleGachaMachineCategories.MachineCategory = CollectibleGachaMachineCategories.MachineCategory(
    id = id,
    name = name,
    machinesQuantity = machinesQuantity,
)

internal fun createCategories(
    categories: List<CollectibleGachaMachineCategories.MachineCategory> = listOf(
        createCategory(id = "sports", name = "Sports", machinesQuantity = 12),
        createCategory(id = "anime", name = "Anime", machinesQuantity = 7),
    ),
    totalMachinesQuantity: Int = 19,
): CollectibleGachaMachineCategories = CollectibleGachaMachineCategories(
    categories = categories,
    totalMachinesQuantity = totalMachinesQuantity,
)

internal fun createBatch(
    key: Int = 0,
    machines: List<CollectibleGachaMachine> = listOf(createMachine()),
    requestCursor: String? = null,
): Batch<Int, CollectibleGachaMachinesBatch> = Batch(
    key = key,
    data = CollectibleGachaMachinesBatch(collectibleGachaMachines = machines, requestCursor = requestCursor),
)

/** Builds the pagination state the transformer and the model read; [batches] is one list of machines per page. */
internal fun createBatchListState(
    batches: List<List<CollectibleGachaMachine>> = listOf(listOf(createMachine())),
    status: PaginationStatus<CollectibleGachaMachinesBatch> = PaginationStatus.EndOfPagination,
    totalCount: Int? = null,
): CollectibleGachaMachinesBatchListState = CollectibleGachaMachinesBatchListState(
    data = batches.mapIndexed { index, machines -> createBatch(key = index, machines = machines) },
    status = status,
    totalCount = totalCount,
)

internal fun createNft(
    id: String = "nft_1",
    nftAddress: String = "GmnqDr9HsNd7o54YksNHrTQGGpFbY7w7aBugL3heUZhZ",
    name: String = "Raging Bolt EX",
    description: String? = "2024 #208 Raging Bolt EX PSA 10",
    rarity: CollectibleRarity? = CollectibleRarity.RARE,
    grade: CollectibleGachaMachineNft.Grade? = CollectibleGachaMachineNft.Grade(company = "PSA", value = "10"),
    imageUrl: String? = "https://tangem.com/nft.png",
    insuredValue: BigDecimal? = BigDecimal("240"),
): CollectibleGachaMachineNft = CollectibleGachaMachineNft(
    id = id,
    nftAddress = nftAddress,
    name = name,
    description = description,
    rarity = rarity,
    grade = grade,
    imageUrl = imageUrl,
    insuredValue = insuredValue,
)

internal fun createNftBatch(
    key: Int = 0,
    nfts: List<CollectibleGachaMachineNft> = listOf(createNft()),
): Batch<Int, List<CollectibleGachaMachineNft>> = Batch(key = key, data = nfts)

/** Builds the prize-pool pagination state; [batches] is one list of collectibles per page. */
internal fun createNftsBatchListState(
    batches: List<List<CollectibleGachaMachineNft>> = listOf(listOf(createNft())),
    status: PaginationStatus<List<CollectibleGachaMachineNft>> = PaginationStatus.EndOfPagination,
    totalCount: Int? = null,
): CollectibleGachaMachineNftsBatchListState = CollectibleGachaMachineNftsBatchListState(
    data = batches.mapIndexed { index, nfts -> createNftBatch(key = index, nfts = nfts) },
    status = status,
    totalCount = totalCount,
)