package com.tangem.features.polymarket.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.model.PolymarketSessionKey
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
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketFastTradingBannerSourceTest {

    private val observeFastTrading: ObservePolymarketFastTradingUseCase = mockk()
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase = mockk()
    private val bannerStore: PolymarketFastTradingBannerStore = mockk()
    private val featureToggles: PolymarketFeatureToggles = mockk()
    private val clock = MutableClock(NOW)

    private val source = PolymarketFastTradingBannerSource(
        observeFastTrading = observeFastTrading,
        refreshFastTrading = refreshFastTrading,
        bannerStore = bannerStore,
        featureToggles = featureToggles,
        clock = clock,
    )

    @BeforeEach
    fun reset() {
        clearMocks(observeFastTrading, refreshFastTrading, bannerStore, featureToggles)
        clock.now = NOW
        every { featureToggles.isPolymarketFastOrdersEnabled } returns true
        every { bannerStore.dismissedAt(any(), any()) } returns flowOf(null)
        coEvery { bannerStore.dismiss(any(), any(), any()) } returns Unit
        coEvery { refreshFastTrading(any()) } returns Unit
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fast trading state WHEN observe THEN emits the expected banner`(model: BannerModel) = runTest {
        // Arrange
        every { observeFastTrading(WALLET_ID) } returns flowOf(model.state)
        every { bannerStore.dismissedAt(WALLET_ID, PolymarketFastTradingBannerKind.ACTIVATE) } returns
            flowOf(model.activateDismissedAt)

        // Act
        val actual = getEmittedValues(source.observe(WALLET_ID, flowOf(Unit)))

        // Assert
        assertThat(actual).containsExactly(model.expected)
    }

    private fun provideTestModels() = listOf(
        BannerModel(state = stateOf(OFFER_ENABLE), expected = PolymarketFastTradingBanner.Activate),
        BannerModel(
            state = stateOf(OFFER_ENABLE),
            activateDismissedAt = NOW.minus(Duration.ofDays(29)),
            expected = null,
        ),
        BannerModel(
            state = stateOf(OFFER_ENABLE),
            activateDismissedAt = NOW.minus(Duration.ofDays(30)),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.Reenable(keyOf())),
            expected = PolymarketFastTradingBanner.Activate,
        ),
        BannerModel(
            state = stateOf(PolymarketSessionKeyDeviceState.RepairByRenewal(keyOf())),
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
            state = stateOf(PolymarketSessionKeyDeviceState.RenewalRequired(keyOf())),
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
            activateDismissedAt = NOW,
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

    @Test
    fun `GIVEN activate banner WHEN dismiss activation THEN stores the clock instant for activate`() = runTest {
        // Arrange
        clock.now = NOW.plusSeconds(42)

        // Act
        source.dismissActivation(WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            bannerStore.dismiss(WALLET_ID, PolymarketFastTradingBannerKind.ACTIVATE, NOW.plusSeconds(42))
        }
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
        val activateDismissedAt: Instant? = null,
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
        ) = PolymarketSessionKey(
            address = address,
            deviceId = "other-device",
            deviceName = "Pixel 9",
            isEnabled = true,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = NOW.plus(Duration.ofDays(100)),
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
            return PolymarketFastTradingState(overview = overview.right(), operation = operation, finished = null)
        }
    }
}