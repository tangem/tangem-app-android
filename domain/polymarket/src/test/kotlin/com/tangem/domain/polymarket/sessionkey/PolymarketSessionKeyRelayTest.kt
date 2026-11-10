package com.tangem.domain.polymarket.sessionkey

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal class PolymarketSessionKeyRelayTest {

    private val sessionKeyRepository: PolymarketSessionKeyRepository = mockk()
    private val polymarketRepository: PolymarketRepository = mockk()
    private val relay = PolymarketSessionKeyRelay(
        sessionKeyRepository = sessionKeyRepository,
        polymarketRepository = polymarketRepository,
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    @BeforeEach
    fun setup() {
        clearMocks(sessionKeyRepository, polymarketRepository)
    }

    @Test
    fun `GIVEN an older pending record listed before the newest active one WHEN settle THEN the newest settles it`() =
        runTest {
            // Arrange
            val submitted = key(PolymarketSessionKeyStatus.PENDING, createdAt = NOW)
            val renewed = key(PolymarketSessionKeyStatus.ACTIVE, createdAt = NOW.plusSeconds(60))
            coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
                wallet(submitted, renewed).right()

            // Act
            val actual = relay.settle(USER_WALLET_ID, submitted)

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyRelay.Settlement.Settled(renewed))
        }

    @Test
    fun `GIVEN a revoked record listed before the newest pending one WHEN settle THEN it waits for the newest`() =
        runTest {
            // Arrange
            val submitted = key(PolymarketSessionKeyStatus.PENDING, createdAt = NOW.plusSeconds(60))
            val revoked = key(PolymarketSessionKeyStatus.REVOKED, createdAt = NOW)
            val activated = submitted.copy(status = PolymarketSessionKeyStatus.ACTIVE)
            coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returnsMany listOf(
                wallet(revoked, submitted).right(),
                wallet(revoked, activated).right(),
            )

            // Act
            val actual = relay.settle(USER_WALLET_ID, submitted)

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyRelay.Settlement.Settled(activated))
        }

    @Test
    fun `GIVEN the record listed in another case WHEN settle THEN it is matched`() = runTest {
        // Arrange
        val submitted = key(PolymarketSessionKeyStatus.PENDING, createdAt = NOW)
        val settled = submitted.copy(address = CHECKSUMMED_ADDRESS, status = PolymarketSessionKeyStatus.ACTIVE)
        coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
            wallet(settled).right()

        // Act
        val actual = relay.settle(USER_WALLET_ID, submitted)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyRelay.Settlement.Settled(settled))
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        const val ADDRESS = "0x5e55000000000000000000000000000000000abc"
        const val CHECKSUMMED_ADDRESS = "0x5E55000000000000000000000000000000000ABC"

        fun wallet(vararg keys: PolymarketSessionKey) = PolymarketWalletState(
            depositWalletAddress = "0x2222222222222222222222222222222222222222",
            status = PolymarketWalletStatus.READY_TO_TRADE,
            sessionKeys = keys.toList(),
        )

        fun key(status: PolymarketSessionKeyStatus, createdAt: Instant) = PolymarketSessionKey(
            address = ADDRESS,
            deviceId = "device",
            deviceName = "Pixel",
            isEnabled = true,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = NOW.plusSeconds(1_000_000),
            renewAfter = null,
            authorizedAt = null,
            createdAt = createdAt,
        )
    }
}