package com.tangem.features.hotwallet.hardwareupgrade

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.storiesv2.GetHardwareUpgradeStoriesVariantUseCase
import com.tangem.features.storiesv2.HardwareUpgradeStoriesVariant
import com.tangem.features.storiesv2.StoriesV2Prefetcher
import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultHardwareUpgradeLauncherTest {

    private val appRouter: AppRouter = mockk()
    private val getHardwareUpgradeStoriesVariantUseCase: GetHardwareUpgradeStoriesVariantUseCase = mockk()
    private val storiesV2Prefetcher: StoriesV2Prefetcher = mockk(relaxUnitFun = true)

    private val launcher = DefaultHardwareUpgradeLauncher(
        appRouter = appRouter,
        getHardwareUpgradeStoriesVariantUseCase = getHardwareUpgradeStoriesVariantUseCase,
        storiesV2Prefetcher = storiesV2Prefetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter, getHardwareUpgradeStoriesVariantUseCase, storiesV2Prefetcher)
        every { appRouter.stack } returns emptyList()
        every { appRouter.push(any(), any()) } just runs
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an experiment variant WHEN launch THEN the route of that variant is opened`(model: VariantModel) =
        runTest {
            // Arrange
            coEvery { getHardwareUpgradeStoriesVariantUseCase() } returns model.variant

            // Act
            launcher.launch(userWalletId = USER_WALLET_ID, source = SOURCE)

            // Assert
            verify(exactly = 1) { appRouter.push(model.expectedRoute, any()) }
        }

    @Test
    fun `GIVEN the route already open WHEN launch again THEN nothing is pushed on top of it`() = runTest {
        // Arrange
        val storiesRoute = AppRoute.HardwareUpgradeStories(userWalletId = USER_WALLET_ID, source = SOURCE)
        coEvery { getHardwareUpgradeStoriesVariantUseCase() } returns HardwareUpgradeStoriesVariant.TREATMENT
        every { appRouter.stack } returns listOf(storiesRoute)

        // Act
        launcher.launch(userWalletId = USER_WALLET_ID, source = SOURCE)

        // Assert
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("providePrefetchModels")
    fun `GIVEN a variant on the device WHEN prefetch THEN only treatment warms the story`(model: PrefetchModel) {
        // Arrange
        every { getHardwareUpgradeStoriesVariantUseCase.peek() } returns model.variant

        // Act
        launcher.prefetch()

        // Assert
        verify(exactly = model.expectedPrefetches) { storiesV2Prefetcher.prefetch(StoryV2Type.HARDWARE_UPGRADE) }
    }

    private fun providePrefetchModels() = listOf(
        PrefetchModel(variant = HardwareUpgradeStoriesVariant.TREATMENT, expectedPrefetches = 1),
        PrefetchModel(variant = HardwareUpgradeStoriesVariant.CONTROL, expectedPrefetches = 0),
    )

    internal data class PrefetchModel(val variant: HardwareUpgradeStoriesVariant, val expectedPrefetches: Int)

    private fun provideTestModels() = listOf(
        VariantModel(
            variant = HardwareUpgradeStoriesVariant.TREATMENT,
            expectedRoute = AppRoute.HardwareUpgradeStories(userWalletId = USER_WALLET_ID, source = SOURCE),
        ),
        VariantModel(
            variant = HardwareUpgradeStoriesVariant.CONTROL,
            expectedRoute = AppRoute.WalletHardwareBackup(userWalletId = USER_WALLET_ID),
        ),
    )

    internal data class VariantModel(val variant: HardwareUpgradeStoriesVariant, val expectedRoute: AppRoute)

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val SOURCE = AppRoute.HardwareUpgradeStories.Source.WALLET_SETTINGS
    }
}