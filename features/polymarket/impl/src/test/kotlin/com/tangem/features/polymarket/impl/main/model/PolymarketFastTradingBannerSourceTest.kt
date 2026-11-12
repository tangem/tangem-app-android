package com.tangem.features.polymarket.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerKind
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessment
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketFastTradingBannerSourceTest {

    private val observeFastTrading: ObservePolymarketFastTradingUseCase = mockk()
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase = mockk()
    private val startOperation: StartPolymarketSessionKeyOperationUseCase = mockk()
    private val bannerStore: PolymarketFastTradingBannerStore = mockk()
    private val featureToggles: PolymarketFeatureToggles = mockk()
    private val clock = MutableClock(NOW)

    private val source = PolymarketFastTradingBannerSource(
        observeFastTrading = observeFastTrading,
        refreshFastTrading = refreshFastTrading,
        startOperation = startOperation,
        bannerStore = bannerStore,
        featureToggles = featureToggles,
        clock = clock,
    )

    @BeforeEach
    fun reset() {
        clearMocks(observeFastTrading, refreshFastTrading, startOperation, bannerStore, featureToggles)
        clock.now = NOW
        every { featureToggles.isPolymarketFastOrdersEnabled } returns true
        every { bannerStore.dismissedAt(any(), any()) } returns flowOf(null)
        coEvery { bannerStore.dismiss(any(), any(), any()) } returns Unit
        coEvery { refreshFastTrading(any()) } returns Unit
        every { startOperation(any(), any()) } returns PolymarketSessionKeyOperationStart.Started(id = 1)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fast trading state WHEN observe THEN emits the expected banner`(model: BannerModel) = runTest {
        // Arrange
        every { observeFastTrading(WALLET_ID) } returns flowOf(model.state)
        model.dismissals.forEach { (kind, dismissedAt) ->
            every { bannerStore.dismissedAt(WALLET_ID, kind) } returns flowOf(dismissedAt)
        }

        // Act
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))

        // Assert
        assertThat(actual).containsExactly(model.expected)
    }

    private fun provideTestModels() = listOf(
        BannerModel(state = stateOf(OFFER_ENABLE), expected = PolymarketFastTradingBanner.Activate),
        BannerModel(
            state = stateOf(OFFER_ENABLE),
            dismissals = mapOf(ACTIVATE to NOW.minus(Duration.ofDays(29))),
            expected = null,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE),
            dismissals = mapOf(ACTIVATE to NOW.minus(Duration.ofDays(30))),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Reenable(keyOf())),
            expected = REACTIVATE,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Reenable(keyOf(status = PolymarketSessionKeyStatus.EXPIRED)),
            ),
            expected = REACTIVATE,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Reenable(keyOf(status = PolymarketSessionKeyStatus.FAILED)),
            ),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.RepairByRenewal(keyOf(status = PolymarketSessionKeyStatus.FAILED)),
            ),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Reenable(keyOf(status = PolymarketSessionKeyStatus.PENDING)),
            ),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Reenable(
                    keyOf(status = PolymarketSessionKeyStatus.PENDING, createdAt = NOW),
                ),
            ),
            expected = REACTIVATE,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Reenable(keyOf(status = PolymarketSessionKeyStatus.FAILED)),
            ),
            dismissals = mapOf(REACTIVATE_KIND to NOW),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Reenable(keyOf())),
            dismissals = mapOf(REACTIVATE_KIND to NOW.minus(Duration.ofHours(23))),
            expected = null,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Reenable(keyOf())),
            dismissals = mapOf(REACTIVATE_KIND to NOW.minus(Duration.ofHours(24))),
            expected = REACTIVATE,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Reenable(keyOf())),
            dismissals = mapOf(ACTIVATE to NOW),
            expected = REACTIVATE,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE),
            dismissals = mapOf(REACTIVATE_KIND to NOW, EXTEND_KIND to NOW),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(30)), false)),
            expected = PolymarketFastTradingBanner.Extend(daysLeft = 30),
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(30).plusSeconds(1)), false),
            ),
            expected = null,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(12).plusHours(1)), false),
            ),
            expected = PolymarketFastTradingBanner.Extend(daysLeft = 13),
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Disabled(keyOf(validFor = Duration.ofDays(2)), true)),
            expected = PolymarketFastTradingBanner.Extend(daysLeft = 2),
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.RenewalRequired(keyOf(validFor = Duration.ofMinutes(5)))),
            expected = PolymarketFastTradingBanner.Extend(daysLeft = 1),
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(5)), false)),
            dismissals = mapOf(EXTEND_KIND to NOW.minus(Duration.ofDays(29))),
            expected = null,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(5)), false)),
            dismissals = mapOf(EXTEND_KIND to NOW.minus(Duration.ofDays(30))),
            expected = PolymarketFastTradingBanner.Extend(daysLeft = 5),
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(5)), false),
                operation = OPERATION.copy(request = PolymarketSessionKeyOperationRequest.Extend),
            ),
            expected = null,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Active(keyOf(validFor = Duration.ofDays(5)), false),
                foreignSigners = listOf(FOREIGN_SIGNER),
            ),
            expected = PolymarketFastTradingBanner.UnknownSigners,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.RepairByRenewal(keyOf(status = PolymarketSessionKeyStatus.FAILED)),
                foreignSigners = listOf(FOREIGN_SIGNER.copy(address = "0xOWN")),
            ),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Replace.ByRevocation(keyOf())),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Active(keyOf(), isRenewDue = false)),
            expected = null,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Active(keyOf(), isRenewDue = true)),
            expected = null,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Disabled(keyOf(), isRenewDue = true)),
            expected = null,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Enabling(keyOf())),
            expected = null,
        ),
        BannerModel(state = stateOf(OFFER_ENABLE, operation = OPERATION), expected = null),
        BannerModel(
            state = stateOf(OFFER_ENABLE, walletStatus = PolymarketWalletStatus.DEPLOYED),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE, walletStatus = PolymarketWalletStatus.UNKNOWN),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = PolymarketFastTradingState(
                overview = PolymarketWalletError.Unauthorized.left(),
                operation = null,
                finished = null,
            ),
            expected = null,
        ),
        BannerModel(
            state = stateOf(
                OFFER_ENABLE,
                sessionKeys = listOf(keyOf(address = "0xother", status = PolymarketSessionKeyStatus.REVOKING)),
            ),
            expected = null,
        ),
        BannerModel(
            state = stateOf(
                OFFER_ENABLE,
                sessionKeys = listOf(
                    keyOf(address = "0xother", status = PolymarketSessionKeyStatus.PENDING, createdAt = NOW),
                ),
            ),
            expected = null,
        ),
        BannerModel(
            state = stateOf(
                OFFER_ENABLE,
                sessionKeys = listOf(
                    keyOf(
                        address = "0xother",
                        status = PolymarketSessionKeyStatus.PENDING,
                        createdAt = NOW.minus(Duration.ofMinutes(10)),
                    ),
                ),
            ),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(
                OFFER_ENABLE,
                sessionKeys = listOf(
                    keyOf(
                        address = "0xother",
                        status = PolymarketSessionKeyStatus.REVOKING,
                        createdAt = NOW.minus(Duration.ofDays(2)),
                    ),
                    keyOf(
                        address = "0xOTHER",
                        status = PolymarketSessionKeyStatus.REVOKED,
                        createdAt = NOW.minus(Duration.ofDays(1)),
                    ),
                ),
            ),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(
                OFFER_ENABLE,
                sessionKeys = listOf(
                    keyOf(
                        address = "0xother",
                        status = PolymarketSessionKeyStatus.REVOKED,
                        createdAt = NOW.minus(Duration.ofDays(2)),
                    ),
                    keyOf(
                        address = "0xother",
                        status = PolymarketSessionKeyStatus.REVOKING,
                        createdAt = NOW.minus(Duration.ofDays(1)),
                    ),
                ),
            ),
            expected = null,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE, foreignSigners = listOf(FOREIGN_SIGNER)),
            expected = PolymarketFastTradingBanner.UnknownSigners,
        ),
        BannerModel(
            state = stateOf(
                PolymarketSessionKeyDeviceState.Active(keyOf(), isRenewDue = false),
                operation = OPERATION,
                foreignSigners = listOf(FOREIGN_SIGNER),
                sessionKeys = listOf(keyOf(address = "0xother", status = PolymarketSessionKeyStatus.REVOKING)),
            ),
            expected = PolymarketFastTradingBanner.UnknownSigners,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE, foreignSigners = listOf(FOREIGN_SIGNER)),
            dismissals = mapOf(ACTIVATE to NOW),
            expected = PolymarketFastTradingBanner.UnknownSigners,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE, isReconciled = false, foreignSigners = listOf(FOREIGN_SIGNER)),
            expected = PolymarketFastTradingBanner.Activate,
        ),
    )

    @Test
    fun `GIVEN toggle off WHEN observe THEN emits no banner and use case is never invoked`() = runTest {
        // Arrange
        every { featureToggles.isPolymarketFastOrdersEnabled } returns false

        // Act
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))

        // Assert
        assertThat(actual).containsExactly(null)
        verify(exactly = 0) { observeFastTrading(any()) }
    }

    @Test
    fun `GIVEN dismissed banner WHEN recheck emitted after the dismissal period THEN banner reappears`() = runTest {
        // Arrange
        val rechecks = MutableSharedFlow<Unit>(replay = 1)
        rechecks.tryEmit(Unit)
        every { observeFastTrading(WALLET_ID) } returns flowOf(stateOf(OFFER_ENABLE))
        every { bannerStore.dismissedAt(WALLET_ID, PolymarketFastTradingBannerKind.ACTIVATE) } returns
            flowOf(NOW.minus(Duration.ofDays(29)))
        val actual = getEmittedValues(source.observe(WALLET_ID, rechecks))

        // Act
        clock.now = NOW.plus(Duration.ofDays(2))
        rechecks.tryEmit(Unit)

        // Assert
        assertThat(actual).containsExactly(null, PolymarketFastTradingBanner.Activate).inOrder()
    }

    @Test
    fun `GIVEN another device key pending for 9 minutes WHEN its settling window ends THEN activate appears`() =
        runTest {
            // Arrange
            val pendingKey = keyOf(
                address = "0xother",
                status = PolymarketSessionKeyStatus.PENDING,
                createdAt = NOW.minus(Duration.ofMinutes(9)),
            )
            every { observeFastTrading(WALLET_ID) } returns
                flowOf(stateOf(OFFER_ENABLE, sessionKeys = listOf(pendingKey)))
            val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))
            runCurrent()
            val beforeWindowEnds = actual.toList()

            // Act
            advanceTimeBy(Duration.ofMinutes(1).toMillis())
            runCurrent()

            // Assert
            assertThat(beforeWindowEnds).containsExactly(null)
            assertThat(actual).containsExactly(null, PolymarketFastTradingBanner.Activate).inOrder()
        }

    @Test
    fun `GIVEN activate dismissed WHEN its dismissal period ends while observed THEN activate appears`() = runTest {
        // Arrange
        every { observeFastTrading(WALLET_ID) } returns flowOf(stateOf(OFFER_ENABLE))
        every { bannerStore.dismissedAt(WALLET_ID, PolymarketFastTradingBannerKind.ACTIVATE) } returns
            flowOf(NOW.minus(Duration.ofDays(30)).plus(Duration.ofHours(1)))
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))
        runCurrent()
        val beforePeriodEnds = actual.toList()

        // Act
        advanceTimeBy(Duration.ofHours(1).toMillis())
        runCurrent()

        // Assert
        assertThat(beforePeriodEnds).containsExactly(null)
        assertThat(actual).containsExactly(null, PolymarketFastTradingBanner.Activate).inOrder()
    }

    @Test
    fun `GIVEN shown banner WHEN recheck emitted without time passing THEN nothing new is emitted`() = runTest {
        // Arrange
        val rechecks = MutableSharedFlow<Unit>(replay = 1)
        rechecks.tryEmit(Unit)
        every { observeFastTrading(WALLET_ID) } returns flowOf(stateOf(OFFER_ENABLE))
        val actual = getEmittedValues(source.observe(WALLET_ID, rechecks))

        // Act
        rechecks.tryEmit(Unit)

        // Assert
        assertThat(actual).containsExactly(PolymarketFastTradingBanner.Activate)
    }

    @ParameterizedTest
    @MethodSource("provideDismissModels")
    fun `GIVEN a banner WHEN dismissed THEN stores the clock instant under its kind`(model: DismissModel) = runTest {
        // Arrange
        clock.now = NOW.plusSeconds(42)

        // Act
        source.dismiss(WALLET_ID, model.banner)

        // Assert
        coVerify(exactly = 1) { bannerStore.dismiss(WALLET_ID, model.kind, NOW.plusSeconds(42)) }
    }

    private fun provideDismissModels() = listOf(
        DismissModel(banner = PolymarketFastTradingBanner.Activate, kind = ACTIVATE),
        DismissModel(banner = REACTIVATE, kind = REACTIVATE_KIND),
        DismissModel(
            banner = PolymarketFastTradingBanner.Reactivate(PolymarketSessionKeyOperationRequest.Activate),
            kind = REACTIVATE_KIND,
        ),
        DismissModel(banner = PolymarketFastTradingBanner.Extend(daysLeft = 3), kind = EXTEND_KIND),
    )

    @Test
    fun `GIVEN unknown signers banner WHEN dismissed THEN nothing is stored`() = runTest {
        // Act
        source.dismiss(WALLET_ID, PolymarketFastTradingBanner.UnknownSigners)

        // Assert
        coVerify(exactly = 0) { bannerStore.dismiss(any(), any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("provideStartModels")
    fun `GIVEN an actionable banner WHEN started THEN its operation is requested`(model: StartModel) {
        // Act
        val actual = source.start(WALLET_ID, model.banner)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyOperationStart.Started(id = 1))
        verify(exactly = 1) { startOperation(WALLET_ID, model.request) }
    }

    private fun provideStartModels() = listOf(
        StartModel(PolymarketFastTradingBanner.Activate, PolymarketSessionKeyOperationRequest.Activate),
        StartModel(REACTIVATE, PolymarketSessionKeyOperationRequest.Reactivate),
        StartModel(
            PolymarketFastTradingBanner.Reactivate(PolymarketSessionKeyOperationRequest.Activate),
            PolymarketSessionKeyOperationRequest.Activate,
        ),
        StartModel(PolymarketFastTradingBanner.Extend(daysLeft = 3), PolymarketSessionKeyOperationRequest.Extend),
    )

    @Test
    fun `GIVEN unknown signers banner WHEN started THEN no operation is requested`() {
        // Act
        val actual = source.start(WALLET_ID, PolymarketFastTradingBanner.UnknownSigners)

        // Assert
        assertThat(actual).isNull()
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN activation runs WHEN it fails THEN activate banner returns at once`() = runTest {
        // Arrange
        val states = MutableStateFlow(stateOf(OFFER_ENABLE, operation = OPERATION))
        every { observeFastTrading(WALLET_ID) } returns states
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))
        runCurrent()

        // Act
        states.value = stateOf(
            PolymarketSessionKeyDeviceState.Reenable(keyOf(status = PolymarketSessionKeyStatus.FAILED)),
            sessionKeys = listOf(keyOf(status = PolymarketSessionKeyStatus.FAILED, createdAt = NOW)),
            finished = PolymarketSessionKeyOperationOutcome(
                id = 1,
                request = PolymarketSessionKeyOperationRequest.Activate,
                result = PolymarketSessionKeyOperationFailure(
                    error = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy),
                    isAfterRevocation = false,
                ).left(),
            ),
        )
        runCurrent()

        // Assert
        assertThat(actual).containsExactly(null, PolymarketFastTradingBanner.Activate).inOrder()
    }

    @Test
    fun `GIVEN own key a day past the extension window WHEN the window opens THEN extend appears`() = runTest {
        // Arrange
        val key = keyOf(validFor = Duration.ofDays(31))
        every { observeFastTrading(WALLET_ID) } returns
            flowOf(stateOf(PolymarketSessionKeyDeviceState.Active(key, isRenewDue = false)))
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))
        runCurrent()

        // Act
        advanceTimeBy(Duration.ofDays(1).toMillis())
        runCurrent()

        // Assert
        assertThat(actual).containsExactly(null, PolymarketFastTradingBanner.Extend(daysLeft = 30)).inOrder()
    }

    @Test
    fun `GIVEN extend banner WHEN a day passes THEN days left decrease`() = runTest {
        // Arrange
        val key = keyOf(validFor = Duration.ofDays(5).plusHours(1))
        every { observeFastTrading(WALLET_ID) } returns
            flowOf(stateOf(PolymarketSessionKeyDeviceState.Active(key, isRenewDue = false)))
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))
        runCurrent()

        // Act
        advanceTimeBy(Duration.ofHours(1).toMillis())
        runCurrent()

        // Assert
        assertThat(actual).containsExactly(
            PolymarketFastTradingBanner.Extend(daysLeft = 6),
            PolymarketFastTradingBanner.Extend(daysLeft = 5),
        ).inOrder()
    }

    @Test
    fun `GIVEN reactivate dismissed WHEN its 24 hours end while observed THEN reactivate appears`() = runTest {
        // Arrange
        every { observeFastTrading(WALLET_ID) } returns
            flowOf(stateOf(PolymarketSessionKeyDeviceState.Reenable(keyOf())))
        every { bannerStore.dismissedAt(WALLET_ID, REACTIVATE_KIND) } returns
            flowOf(NOW.minus(Duration.ofHours(23)))
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))
        runCurrent()

        // Act
        advanceTimeBy(Duration.ofHours(1).toMillis())
        runCurrent()

        // Assert
        assertThat(actual).containsExactly(null, REACTIVATE).inOrder()
    }

    @Test
    fun `GIVEN toggle on WHEN refresh THEN delegates to the use case`() = runTest {
        // Act
        source.refresh(WALLET_ID)

        // Assert
        coVerify(exactly = 1) { refreshFastTrading(WALLET_ID) }
    }

    @Test
    fun `GIVEN toggle off WHEN refresh THEN use case is not invoked`() = runTest {
        // Arrange
        every { featureToggles.isPolymarketFastOrdersEnabled } returns false

        // Act
        source.refresh(WALLET_ID)

        // Assert
        coVerify(exactly = 0) { refreshFastTrading(any()) }
    }

    internal data class BannerModel(
        val state: PolymarketFastTradingState,
        val expected: PolymarketFastTradingBanner?,
        val dismissals: Map<PolymarketFastTradingBannerKind, Instant> = emptyMap(),
    )

    internal data class DismissModel(
        val banner: PolymarketFastTradingBanner,
        val kind: PolymarketFastTradingBannerKind,
    )

    internal data class StartModel(
        val banner: PolymarketFastTradingBanner,
        val request: PolymarketSessionKeyOperationRequest,
    )

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    private companion object {
        val NOW: Instant = Instant.ofEpochSecond(1_794_182_400)
        val WALLET_ID = UserWalletId("022")
        val OFFER_ENABLE = PolymarketSessionKeyDeviceState.OfferEnable
        val ACTIVATE = PolymarketFastTradingBannerKind.ACTIVATE
        val REACTIVATE_KIND = PolymarketFastTradingBannerKind.REACTIVATE
        val EXTEND_KIND = PolymarketFastTradingBannerKind.EXTEND
        val REACTIVATE = PolymarketFastTradingBanner.Reactivate(PolymarketSessionKeyOperationRequest.Reactivate)
        val FOREIGN_SIGNER = PolymarketSessionSigner(address = "0xforeign", scopes = listOf("CLOB"), validUntil = null)
        val OPERATION = PolymarketSessionKeyOperation(
            id = 1,
            request = PolymarketSessionKeyOperationRequest.Activate,
            targetAddress = null,
            stage = PolymarketSessionKeyOperationStage.SIGNING,
        )

        fun keyOf(
            address: String = "0xown",
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            createdAt: Instant = NOW.minus(Duration.ofDays(10)),
            validFor: Duration = Duration.ofDays(100),
        ) = PolymarketSessionKey(
            address = address,
            deviceId = "other-device",
            deviceName = "Pixel 9",
            isEnabled = true,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = NOW.plus(validFor),
            renewAfter = null,
            authorizedAt = null,
            createdAt = createdAt,
        )

        fun stateOf(
            deviceState: PolymarketSessionKeyDeviceState,
            operation: PolymarketSessionKeyOperation? = null,
            walletStatus: PolymarketWalletStatus = PolymarketWalletStatus.READY_TO_TRADE,
            foreignSigners: List<PolymarketSessionSigner> = emptyList(),
            isReconciled: Boolean = true,
            sessionKeys: List<PolymarketSessionKey> = emptyList(),
            finished: PolymarketSessionKeyOperationOutcome? = null,
        ): PolymarketFastTradingState {
            val overview = PolymarketSessionKeyOverview(
                wallet = PolymarketWalletState(
                    depositWalletAddress = "0xdeposit",
                    status = walletStatus,
                    sessionKeys = sessionKeys,
                ),
                localKeyAddress = "0xown",
                deviceId = "this-device",
                assessment = PolymarketSessionKeyAssessment(
                    deviceState = deviceState,
                    reconciliation = if (isReconciled) {
                        PolymarketSessionKeyReconciliation.Checked(
                            foreignSigners = foreignSigners,
                            missingOnExchange = emptySet(),
                            listedSigners = emptySet(),
                        )
                    } else {
                        PolymarketSessionKeyReconciliation.Unavailable
                    },
                    nextChangeAt = null,
                ),
                isTradable = true,
            )
            return PolymarketFastTradingState(overview = overview.right(), operation = operation, finished = finished)
        }
    }
}