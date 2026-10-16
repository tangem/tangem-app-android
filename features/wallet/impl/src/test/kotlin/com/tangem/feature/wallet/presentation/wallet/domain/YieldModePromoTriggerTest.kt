package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnYieldOffers
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics.PromoScreenSkipped
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics.SkipReason
import com.tangem.features.yield.supply.api.promobottomsheet.GetYieldModePromoVariantUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.ResolveYieldModePromoUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoOffer
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoResolution
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoVariant
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.earn.MockEarn
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldModePromoTriggerTest {

    private val featureToggles: YieldSupplyFeatureToggles = mockk()
    private val yieldSupplyRepository: YieldSupplyRepository = mockk()
    private val earnRepository: EarnRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val resolveYieldModePromoUseCase: ResolveYieldModePromoUseCase = mockk()
    private val getYieldModePromoVariantUseCase: GetYieldModePromoVariantUseCase = mockk()
    private val walletDeepLinkActionListener: WalletDeepLinkActionListener = mockk()
    private val analytics: AnalyticsEventHandler = mockk()

    private val sentEvents = mutableListOf<AnalyticsEvent>()
    private val isOpenedByLinkOrPush = MutableStateFlow(false)
    private val isSurfaceBusy = MutableStateFlow(false)

    private val userWalletId = UserWalletId("011")
    private val statuses = listOf<CryptoCurrencyStatus>(mockk())
    private val statusList: AccountStatusList = mockk { every { flattenPersonalCurrencies() } returns statuses }
    private val earnOffers = EarnYieldOffers(total = 36, best = MockEarn.yieldToken())
    private val yieldSupplyApy = mapOf("1_0xtoken" to BigDecimal("3.35"))
    private val userMaxApyOffer = offer(apy = "3.35", tokenSymbol = "USDC")
    private val globalMaxApyOffer = offer(apy = "9.20", tokenSymbol = "USDT")
    private val ready = YieldModePromoResolution.Ready(userMaxApy = userMaxApyOffer, globalMaxApy = globalMaxApyOffer)

    private var yieldSupplyApyResult: Either<Throwable, Map<String, BigDecimal>> = yieldSupplyApy.right()
    private var isBusyOutsideSlot = false
    private var busyOutsideSlotChecks = 0

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            featureToggles,
            yieldSupplyRepository,
            earnRepository,
            singleAccountStatusListSupplier,
            resolveYieldModePromoUseCase,
            getYieldModePromoVariantUseCase,
            walletDeepLinkActionListener,
            analytics,
        )
        sentEvents.clear()
        isOpenedByLinkOrPush.value = false
        isSurfaceBusy.value = false
        yieldSupplyApyResult = yieldSupplyApy.right()
        isBusyOutsideSlot = false
        busyOutsideSlotChecks = 0

        every { featureToggles.isYieldModePromoEnabled } returns true
        every { walletDeepLinkActionListener.isOpenedByLinkOrPush } returns isOpenedByLinkOrPush
        coEvery { yieldSupplyRepository.isYieldModePromoBottomSheetShown() } returns false
        coEvery { earnRepository.getYieldOffers() } returns earnOffers.right()
        every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(statusList)
        resolveTo(ready)
        coEvery { getYieldModePromoVariantUseCase() } returns YieldModePromoVariant.USER_MAX_APY
        every { analytics.send(capture(sentEvents)) } just runs
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a silent gate closed WHEN findOffer THEN null without a request or an event`(model: SilentGateModel) =
        runTest {
            // Arrange
            model.close()

            // Act
            val actual = createTrigger().findOfferWithDefaults()

            // Assert
            assertThat(actual).isNull()
            assertThat(sentEvents).isEmpty()
            assertThat(busyOutsideSlotChecks).isEqualTo(0)
            coVerify(exactly = 0) { earnRepository.getYieldOffers() }
        }

    @Test
    fun `GIVEN a finished check WHEN findOffer again in the same process THEN null without a request or an event`() =
        runTest {
            // Arrange
            val trigger = createTrigger()
            trigger.findOfferWithDefaults()

            // Act
            val actual = trigger.findOfferWithDefaults()

            // Assert
            assertThat(actual).isNull()
            assertThat(sentEvents).isEmpty()
            coVerify(exactly = 1) { earnRepository.getYieldOffers() }
        }

    @ParameterizedTest
    @MethodSource("provideVariantModels")
    fun `GIVEN a ready offer WHEN findOffer THEN the variant picks the arm`(model: VariantModel) = runTest {
        // Arrange
        coEvery { getYieldModePromoVariantUseCase() } returns model.variant

        // Act
        val actual = createTrigger().findOfferWithDefaults()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        assertThat(sentEvents).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("provideBusyAtStartModels")
    fun `GIVEN the surface busy at the start WHEN findOffer THEN skipped as busy without a request`(
        model: BusyAtStartModel,
    ) = runTest {
        // Arrange
        model.arrange()

        // Act
        val actual = createTrigger().findOfferWithDefaults()

        // Assert
        assertThat(actual).isNull()
        assertThat(sentEvents).containsExactly(PromoScreenSkipped(SkipReason.SurfaceBusy))
        coVerify(exactly = 0) { earnRepository.getYieldOffers() }
    }

    @ParameterizedTest
    @MethodSource("provideSkipModels")
    fun `GIVEN the check fails WHEN findOffer THEN skipped with the reason and the variant is not read`(
        model: SkipModel,
    ) = runTest {
        // Arrange
        model.arrange()

        // Act
        val actual = createTrigger().findOfferWithDefaults()

        // Assert
        assertThat(actual).isNull()
        assertThat(sentEvents).containsExactly(PromoScreenSkipped(model.expected))
        coVerify(exactly = 0) { getYieldModePromoVariantUseCase() }
    }

    @Test
    fun `GIVEN balances loading WHEN they load THEN the offer is returned`() = runTest {
        // Arrange
        val loadedStatusList: AccountStatusList = mockk { every { flattenPersonalCurrencies() } returns statuses }
        val statusLists = MutableStateFlow(statusList)
        every { singleAccountStatusListSupplier(userWalletId) } returns statusLists
        every { resolveYieldModePromoUseCase(statuses, yieldSupplyApy, earnOffers) } returnsMany listOf(
            YieldModePromoResolution.Waiting,
            ready,
        )
        val result = async { createTrigger().findOfferWithDefaults() }
        runCurrent()

        // Act
        statusLists.value = loadedStatusList

        // Assert
        assertThat(result.await()).isEqualTo(userMaxApyOffer)
        assertThat(sentEvents).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("provideWaitingEndModels")
    fun `GIVEN the check waiting WHEN it is cut short THEN skipped with the reason`(model: WaitingEndModel) = runTest {
        // Arrange
        val interruptions = MutableSharedFlow<Unit>()
        model.hang()
        val result = async { createTrigger().findOfferWithDefaults(interruptions = interruptions) }
        runCurrent()

        // Act
        model.cutShort(interruptions)

        // Assert
        assertThat(result.await()).isNull()
        assertThat(sentEvents).containsExactly(PromoScreenSkipped(model.expected))
    }

    @Test
    fun `GIVEN the surface gets busy as the offer is ready WHEN findOffer THEN skipped as busy before the variant`() =
        runTest {
            // Arrange
            every { resolveYieldModePromoUseCase(statuses, yieldSupplyApy, earnOffers) } answers {
                isSurfaceBusy.value = true
                ready
            }

            // Act
            val actual = createTrigger().findOfferWithDefaults()

            // Assert
            assertThat(actual).isNull()
            assertThat(sentEvents).containsExactly(PromoScreenSkipped(SkipReason.SurfaceBusy))
            coVerify(exactly = 0) { getYieldModePromoVariantUseCase() }
        }

    private fun provideTestModels() = listOf(
        SilentGateModel(close = { every { featureToggles.isYieldModePromoEnabled } returns false }),
        SilentGateModel(close = { isOpenedByLinkOrPush.value = true }),
        SilentGateModel(close = { coEvery { yieldSupplyRepository.isYieldModePromoBottomSheetShown() } returns true }),
    )

    private fun provideBusyAtStartModels() = listOf(
        BusyAtStartModel(name = "a dialog shown", arrange = { isSurfaceBusy.value = true }),
        BusyAtStartModel(name = "a sheet outside the slot expected", arrange = { isBusyOutsideSlot = true }),
    )

    private fun provideVariantModels() = listOf(
        VariantModel(variant = YieldModePromoVariant.USER_MAX_APY, expected = userMaxApyOffer),
        VariantModel(variant = YieldModePromoVariant.GLOBAL_MAX_APY, expected = globalMaxApyOffer),
        VariantModel(variant = YieldModePromoVariant.CONTROL, expected = null),
    )

    private fun provideSkipModels() = listOf(
        SkipModel(SkipReason.DataNotReady, arrange = { yieldSupplyApyResult = IllegalStateException().left() }),
        SkipModel(
            SkipReason.DataNotReady,
            arrange = { coEvery { earnRepository.getYieldOffers() } returns IllegalStateException().left() },
        ),
        SkipModel(SkipReason.DataNotReady, arrange = { resolveTo(YieldModePromoResolution.DataNotReady) }),
        SkipModel(SkipReason.NoEligibleToken, arrange = { resolveTo(YieldModePromoResolution.NoEligibleToken) }),
        SkipModel(SkipReason.DataNotReady, arrange = { resolveTo(YieldModePromoResolution.Waiting) }),
    )

    private fun provideWaitingEndModels() = listOf(
        WaitingEndModel(SkipReason.Interrupted, hang = ::hangOnBalances, cutShort = { it.emit(Unit) }),
        WaitingEndModel(
            SkipReason.Interrupted,
            hang = ::hangOnBalances,
            cutShort = { isOpenedByLinkOrPush.value = true },
        ),
        WaitingEndModel(SkipReason.SurfaceBusy, hang = ::hangOnBalances, cutShort = { isSurfaceBusy.value = true }),
        WaitingEndModel(SkipReason.Interrupted, hang = ::hangOnEarnRequest, cutShort = { it.emit(Unit) }),
        WaitingEndModel(SkipReason.Interrupted, hang = ::hangOnVariant, cutShort = { it.emit(Unit) }),
    )

    internal data class SilentGateModel(val close: () -> Unit)

    internal data class BusyAtStartModel(val name: String, val arrange: () -> Unit) {
        override fun toString() = name
    }

    internal data class VariantModel(val variant: YieldModePromoVariant, val expected: YieldModePromoOffer?)

    internal data class SkipModel(val expected: SkipReason, val arrange: () -> Unit)

    internal data class WaitingEndModel(
        val expected: SkipReason,
        val hang: () -> Unit,
        val cutShort: suspend (MutableSharedFlow<Unit>) -> Unit,
    )

    private fun hangOnBalances() {
        every { singleAccountStatusListSupplier(userWalletId) } returns MutableStateFlow(statusList)
        resolveTo(YieldModePromoResolution.Waiting)
    }

    private fun hangOnEarnRequest() {
        coEvery { earnRepository.getYieldOffers() } coAnswers { awaitCancellation() }
    }

    private fun hangOnVariant() {
        coEvery { getYieldModePromoVariantUseCase() } coAnswers { awaitCancellation() }
    }

    private fun resolveTo(resolution: YieldModePromoResolution) {
        every { resolveYieldModePromoUseCase(statuses, yieldSupplyApy, earnOffers) } returns resolution
    }

    private suspend fun YieldModePromoTrigger.findOfferWithDefaults(
        interruptions: MutableSharedFlow<Unit> = MutableSharedFlow(),
    ) = findOffer(
        userWalletId = userWalletId,
        yieldSupplyApy = { yieldSupplyApyResult },
        isSurfaceBusy = isSurfaceBusy,
        isBusyOutsideSlot = {
            busyOutsideSlotChecks++
            isBusyOutsideSlot
        },
        interruptions = interruptions,
    )

    private fun createTrigger() = YieldModePromoTrigger(
        yieldSupplyFeatureToggles = featureToggles,
        yieldSupplyRepository = yieldSupplyRepository,
        earnRepository = earnRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        resolveYieldModePromoUseCase = resolveYieldModePromoUseCase,
        getYieldModePromoVariantUseCase = getYieldModePromoVariantUseCase,
        walletDeepLinkActionListener = walletDeepLinkActionListener,
        analyticsEventHandler = analytics,
    )

    private fun offer(apy: String, tokenSymbol: String) = YieldModePromoOffer(
        apy = BigDecimal(apy),
        tokensCount = 36,
        tokenSymbol = tokenSymbol,
        blockchain = "Ethereum",
    )
}