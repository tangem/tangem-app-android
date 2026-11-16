package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionKeyTradabilityTest {

    private val tradingStore: PolymarketSessionKeyTradingStore = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(tradingStore)
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } returns false
        every { tradingStore.isMissingOnExchange(USER_WALLET_ID, KEY) } returns false
        coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) } returns true
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the key state WHEN tradability THEN the key trades only when every condition holds`(
        model: TradabilityModel,
    ) = runTest {
        // Arrange
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } returns model.isRefused
        every { tradingStore.isMissingOnExchange(USER_WALLET_ID, KEY) } returns model.isMissing
        coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) } returns model.isReady

        // Act
        val actual = tradingStore.tradability(
            userWalletId = USER_WALLET_ID,
            sessionKeyAddress = KEY,
            clock = CLOCK,
            readWallet = { model.wallet },
        )

        // Assert
        assertThat(actual is PolymarketSessionKeyTradability.Tradable).isEqualTo(model.isTradable)
    }

    @Test
    fun `GIVEN a refused key WHEN tradability THEN the wallet is not read`() = runTest {
        // Arrange
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } returns true
        var isWalletRead = false

        // Act
        tradingStore.tradability(
            userWalletId = USER_WALLET_ID,
            sessionKeyAddress = KEY,
            clock = CLOCK,
            readWallet = {
                isWalletRead = true
                wallet().right()
            },
        )

        // Assert
        assertThat(isWalletRead).isFalse()
    }

    @Test
    fun `GIVEN a tradable key WHEN tradability THEN its validUntil is returned`() = runTest {
        // Act
        val actual = tradingStore.tradability(
            userWalletId = USER_WALLET_ID,
            sessionKeyAddress = KEY,
            clock = CLOCK,
            readWallet = { wallet().right() },
        )

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyTradability.Tradable(validUntil = VALID_UNTIL))
    }

    private fun provideTestModels() = listOf(
        TradabilityModel(case = "every condition holds", isTradable = true),
        TradabilityModel(case = "key refused by the exchange", isRefused = true),
        TradabilityModel(case = "key gone from Polymarket", isMissing = true),
        TradabilityModel(case = "not listed since its authorization", isReady = false),
        TradabilityModel(case = "wallet not read", wallet = PolymarketWalletError.Network.left()),
        TradabilityModel(
            case = "wallet not ready to trade",
            wallet = wallet(status = PolymarketWalletStatus.DEPLOYED).right(),
        ),
        TradabilityModel(case = "no record of the key", wallet = wallet(keys = emptyList()).right()),
        TradabilityModel(
            case = "record matched ignoring case",
            wallet = wallet(keys = listOf(key(address = KEY.uppercase().replace("0X", "0x")))).right(),
            isTradable = true,
        ),
        TradabilityModel(
            case = "record pending",
            wallet = wallet(keys = listOf(key(status = PolymarketSessionKeyStatus.PENDING))).right(),
        ),
        TradabilityModel(case = "record disabled", wallet = wallet(keys = listOf(key(isEnabled = false))).right()),
        TradabilityModel(
            case = "record without validUntil",
            wallet = wallet(keys = listOf(key(validUntil = null))).right(),
        ),
        TradabilityModel(
            case = "record expiring exactly in ten minutes",
            wallet = wallet(keys = listOf(key(validUntil = NOW + Duration.ofMinutes(10)))).right(),
        ),
    )

    data class TradabilityModel(
        val case: String,
        val wallet: Either<PolymarketWalletError, PolymarketWalletState> = wallet().right(),
        val isRefused: Boolean = false,
        val isMissing: Boolean = false,
        val isReady: Boolean = true,
        val isTradable: Boolean = false,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val KEY = "0x5e55000000000000000000000000000000000001"
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        val VALID_UNTIL: Instant = NOW.plus(Duration.ofDays(170))
        val CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

        fun wallet(
            status: PolymarketWalletStatus = PolymarketWalletStatus.READY_TO_TRADE,
            keys: List<PolymarketSessionKey> = listOf(key()),
        ) = PolymarketWalletState(
            depositWalletAddress = "0x2222222222222222222222222222222222222222",
            status = status,
            sessionKeys = keys,
        )

        fun key(
            address: String = KEY,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            isEnabled: Boolean = true,
            validUntil: Instant? = VALID_UNTIL,
        ) = PolymarketSessionKey(
            address = address,
            deviceId = "device",
            deviceName = "Pixel",
            isEnabled = isEnabled,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = validUntil,
            renewAfter = null,
            authorizedAt = null,
            createdAt = NOW,
        )
    }
}