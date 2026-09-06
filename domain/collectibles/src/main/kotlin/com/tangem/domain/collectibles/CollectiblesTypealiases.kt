package com.tangem.domain.collectibles

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