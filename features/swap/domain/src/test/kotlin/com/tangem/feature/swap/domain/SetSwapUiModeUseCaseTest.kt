package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SetSwapUiModeUseCaseTest {

    private val swapRepository: SwapRepository = mockk(relaxUnitFun = true)

    private val useCase = SetSwapUiModeUseCase(swapRepository = swapRepository)

    @Test
    fun `GIVEN Simple mode WHEN invoke THEN delegates to repository`() = runTest {
        useCase.invoke(SwapUIMode.Simple)

        coVerify(exactly = 1) { swapRepository.storeSwapUiMode(SwapUIMode.Simple) }
    }

    @Test
    fun `GIVEN Detailed mode WHEN invoke THEN delegates to repository`() = runTest {
        useCase.invoke(SwapUIMode.Detailed)

        coVerify(exactly = 1) { swapRepository.storeSwapUiMode(SwapUIMode.Detailed) }
    }
}