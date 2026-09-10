package com.tangem.features.collectibles.impl.discovery

import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchListState
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
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
    rare: Int = 20,
    epic: Int = 9,
    legendary: Int? = 1,
): CollectibleGachaMachine.Odds = CollectibleGachaMachine.Odds(
    common = common,
    rare = rare,
    epic = epic,
    legendary = legendary,
)

internal fun createStock(
    common: Int = 100,
    rare: Int = 50,
    epic: Int = 10,
    legendary: Int? = 1,
): CollectibleGachaMachine.Stock = CollectibleGachaMachine.Stock(
    common = common,
    rare = rare,
    epic = epic,
    legendary = legendary,
)

internal fun createTierRanges(
    common: CollectibleGachaMachine.TierRange = CollectibleGachaMachine.TierRange(start = 1, end = 10),
    rare: CollectibleGachaMachine.TierRange = CollectibleGachaMachine.TierRange(start = 11, end = 50),
    epic: CollectibleGachaMachine.TierRange = CollectibleGachaMachine.TierRange(start = 51, end = 200),
    legendary: CollectibleGachaMachine.TierRange? = CollectibleGachaMachine.TierRange(start = 201, end = 1000),
): CollectibleGachaMachine.TierRanges = CollectibleGachaMachine.TierRanges(
    common = common,
    rare = rare,
    epic = epic,
    legendary = legendary,
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