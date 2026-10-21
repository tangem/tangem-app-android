package com.tangem.features.polymarket.impl.common.region

import arrow.core.right
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

internal fun createPredictionRegionAccess(isBlocked: Boolean? = null): PredictionRegionAccess {
    val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()
    val access = PredictionRegionAccess(
        checkPolymarketGeoblockUseCase = checkGeoblock,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    if (isBlocked != null) {
        coEvery { checkGeoblock() } returns isBlocked.right()
        runBlocking { access.refresh() }
    }

    return access
}