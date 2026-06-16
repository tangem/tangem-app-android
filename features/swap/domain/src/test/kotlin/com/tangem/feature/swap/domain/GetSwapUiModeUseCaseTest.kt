package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import com.tangem.features.swap.SwapFeatureToggles
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetSwapUiModeUseCaseTest {

    private val swapFeatureToggles: SwapFeatureToggles = mockk()
    private val swapRepository: SwapRepository = mockk()
    private val abTestsManager: ABTestsManager = mockk()

    private val sut = GetSwapUiModeUseCase(
        swapFeatureToggles = swapFeatureToggles,
        swapRepository = swapRepository,
        abTestsManager = abTestsManager,
    )

    @Test
    fun `GIVEN feature toggle is disabled WHEN invoke THEN returns Detailed without reading repository or AB tests`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns false

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Detailed)
            coVerify(exactly = 0) { swapRepository.getStoredSwapUiMode() }
            coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
        }

    @Test
    fun `GIVEN toggle enabled and repository has Detailed WHEN invoke THEN returns Detailed without reading AB tests`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
            coEvery { swapRepository.getStoredSwapUiMode() } returns SwapUIMode.Detailed

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Detailed)
            coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
        }

    @Test
    fun `GIVEN toggle enabled and repository has Simple WHEN invoke THEN returns Simple without reading AB tests`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
            coEvery { swapRepository.getStoredSwapUiMode() } returns SwapUIMode.Simple

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Simple)
            coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
        }

    @Test
    fun `GIVEN toggle enabled and repository empty and AB returns detailed WHEN invoke THEN returns Detailed`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
            coEvery { swapRepository.getStoredSwapUiMode() } returns null
            coEvery { abTestsManager.getValue("swap_form_variant", "detailed") } returns "detailed"

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Detailed)
            coVerify(exactly = 1) { abTestsManager.getValue("swap_form_variant", "detailed") }
        }

    @Test
    fun `GIVEN toggle enabled and repository empty and AB returns simple WHEN invoke THEN returns Simple`() = runTest {
        coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
        coEvery { swapRepository.getStoredSwapUiMode() } returns null
        coEvery { abTestsManager.getValue("swap_form_variant", "detailed") } returns "simple"

        val actual = sut.invoke()

        assertThat(actual).isEqualTo(SwapUIMode.Simple)
        coVerify(exactly = 1) { abTestsManager.getValue("swap_form_variant", "detailed") }
    }

    @Test
    fun `GIVEN toggle enabled and repository empty and AB returns SIMPLE uppercase WHEN invoke THEN returns Simple`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
            coEvery { swapRepository.getStoredSwapUiMode() } returns null
            coEvery { abTestsManager.getValue("swap_form_variant", "detailed") } returns "SIMPLE"

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Simple)
        }

    @Test
    fun `GIVEN toggle enabled and repository empty and AB returns unknown variant WHEN invoke THEN returns Detailed`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
            coEvery { swapRepository.getStoredSwapUiMode() } returns null
            coEvery { abTestsManager.getValue("swap_form_variant", "detailed") } returns "something_else"

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Detailed)
        }

    @Test
    fun `GIVEN toggle enabled and repository empty and AB returns empty string WHEN invoke THEN returns Detailed`() =
        runTest {
            coEvery { swapFeatureToggles.isSwapAbEnabled } returns true
            coEvery { swapRepository.getStoredSwapUiMode() } returns null
            coEvery { abTestsManager.getValue("swap_form_variant", "detailed") } returns ""

            val actual = sut.invoke()

            assertThat(actual).isEqualTo(SwapUIMode.Detailed)
        }
}