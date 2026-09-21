package com.tangem.domain.collectibles

import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchingContext

typealias CollectibleGachaMachinesBatchingContext = BatchingContext<Int, CollectibleGachaMachinesListConfig, Nothing>

typealias CollectibleGachaMachinesBatchAction = BatchAction<Int, CollectibleGachaMachinesListConfig, Nothing>

typealias CollectibleGachaMachinesBatchFlow = BatchFlow<Int, CollectibleGachaMachinesBatch, Nothing>

typealias CollectibleGachaMachinesBatchListState = BatchListState<Int, CollectibleGachaMachinesBatch>

typealias CollectibleGachaMachineNftsBatchingContext =
    BatchingContext<Int, CollectibleGachaMachineNftsListConfig, Nothing>

typealias CollectibleGachaMachineNftsBatchAction = BatchAction<Int, CollectibleGachaMachineNftsListConfig, Nothing>

typealias CollectibleGachaMachineNftsBatchFlow = BatchFlow<Int, List<CollectibleGachaMachineNft>, Nothing>

typealias CollectibleGachaMachineNftsBatchListState = BatchListState<Int, List<CollectibleGachaMachineNft>>