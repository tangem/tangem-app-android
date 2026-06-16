package com.tangem.common.ui.components.currency.icon.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CryptoCurrencyToIconStateConverterTest {

    private val sut = CryptoCurrencyToIconStateConverter(isAvailable = true)
    private val sutUnavailable = CryptoCurrencyToIconStateConverter(isAvailable = false)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.common.ui.extensions.NetworkIconExtKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // region public API — convert(value: CryptoCurrencyStatus)

    @Test
    fun `GIVEN coin status WHEN convert THEN return CoinIcon with currency and network fields`() {
        val coin = buildCoin(
            isTestnet = false,
            isCustom = false,
            iconUrl = "https://example.com/eth.png",
        )
        val status = buildStatus(currency = coin, isError = false)

        val result = sut.convert(status)

        assertThat(result).isEqualTo(
            CurrencyIconState.CoinIcon(
                url = "https://example.com/eth.png",
                fallbackResId = NETWORK_ICON_RES_ID,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        )
    }

    @Test
    fun `GIVEN token status WHEN convert THEN return TokenIcon with currency and network fields`() {
        val token = buildToken(
            isTestnet = false,
            isCustom = false,
            iconUrl = "https://example.com/usdt.png",
            contractAddress = USDT_CONTRACT,
        )
        val status = buildStatus(currency = token, isError = false)

        val result = sut.convert(status) as CurrencyIconState.TokenIcon

        assertThat(result.url).isEqualTo("https://example.com/usdt.png")
        assertThat(result.topBadgeIconResId).isEqualTo(NETWORK_ICON_RES_ID)
        assertThat(result.isGrayscale).isFalse()
        assertThat(result.shouldShowCustomBadge).isFalse()
    }

    // endregion

    // region public API — convert(currency: CryptoCurrency)

    @Test
    fun `GIVEN coin currency WHEN convert without status THEN return CoinIcon with isUnreachable=false`() {
        val coin = buildCoin(isTestnet = false, isCustom = false, iconUrl = "url")

        val result = sut.convert(currency = coin) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isFalse()
        assertThat(result.url).isEqualTo("url")
    }

    @Test
    fun `GIVEN token currency WHEN convert without status THEN return TokenIcon with isErrorStatus=false`() {
        val token = buildToken(
            isTestnet = false,
            isCustom = false,
            iconUrl = "url",
            contractAddress = USDT_CONTRACT,
        )

        val result = sut.convert(currency = token) as CurrencyIconState.TokenIcon

        assertThat(result.isGrayscale).isFalse()
        assertThat(result.url).isEqualTo("url")
    }

    // endregion

    // region public API — convertCustom

    @Test
    fun `GIVEN coin status with custom flag WHEN convertCustom with forceGrayscale and badge off THEN both flags propagate`() {
        val coin = buildCoin(isTestnet = false, isCustom = true, iconUrl = "url")
        val status = buildStatus(currency = coin, isError = false)

        val result = sut.convertCustom(
            value = status,
            forceGrayscale = true,
            showCustomTokenBadge = false,
        ) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isTrue()
        assertThat(result.shouldShowCustomBadge).isFalse()
    }

    @Test
    fun `GIVEN token status WHEN convertCustom with forceGrayscale THEN TokenIcon is grayscale`() {
        val token = buildToken(
            isTestnet = false,
            isCustom = false,
            iconUrl = "url",
            contractAddress = USDT_CONTRACT,
        )
        val status = buildStatus(currency = token, isError = false)

        val result = sut.convertCustom(
            value = status,
            forceGrayscale = true,
            showCustomTokenBadge = true,
        ) as CurrencyIconState.TokenIcon

        assertThat(result.isGrayscale).isTrue()
    }

    // endregion

    // region getIconStateForCoin — isGrayscale matrix

    @Test
    fun `GIVEN no override and live data WHEN convert coin THEN isGrayscale is false`() {
        val coin = buildCoin(isTestnet = false, isCustom = false, iconUrl = "url")
        val status = buildStatus(currency = coin, isError = false)

        val result = sut.convert(status) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isFalse()
    }

    @Test
    fun `GIVEN testnet network WHEN convert coin THEN isGrayscale is true`() {
        val coin = buildCoin(isTestnet = true, isCustom = false, iconUrl = "url")
        val status = buildStatus(currency = coin, isError = false)

        val result = sut.convert(status) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isTrue()
    }

    @Test
    fun `GIVEN error status WHEN convert coin THEN isGrayscale is true`() {
        val coin = buildCoin(isTestnet = false, isCustom = false, iconUrl = "url")
        val status = buildStatus(currency = coin, isError = true)

        val result = sut.convert(status) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isTrue()
    }

    @Test
    fun `GIVEN converter not available WHEN convert coin THEN isGrayscale is true`() {
        val coin = buildCoin(isTestnet = false, isCustom = false, iconUrl = "url")
        val status = buildStatus(currency = coin, isError = false)

        val result = sutUnavailable.convert(status) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isTrue()
    }

    @Test
    fun `GIVEN forceGrayscale flag WHEN convertCustom coin THEN isGrayscale is true`() {
        val coin = buildCoin(isTestnet = false, isCustom = false, iconUrl = "url")
        val status = buildStatus(currency = coin, isError = false)

        val result = sut.convertCustom(
            value = status,
            forceGrayscale = true,
            showCustomTokenBadge = true,
        ) as CurrencyIconState.CoinIcon

        assertThat(result.isGrayscale).isTrue()
    }

    // endregion

    // region getIconStateForToken — branches

    @Test
    fun `GIVEN custom token without iconUrl WHEN convert THEN return CustomTokenIcon`() {
        val token = buildToken(
            isTestnet = false,
            isCustom = true,
            iconUrl = null,
            contractAddress = USDT_CONTRACT,
        )
        val status = buildStatus(currency = token, isError = false)

        val result = sut.convert(status) as CurrencyIconState.CustomTokenIcon

        assertThat(result.topBadgeIconResId).isEqualTo(NETWORK_ICON_RES_ID)
        assertThat(result.isGrayscale).isFalse()
        assertThat(result.shouldShowCustomBadge).isTrue()
    }

    @Test
    fun `GIVEN custom token with iconUrl WHEN convert THEN return TokenIcon with custom badge`() {
        val token = buildToken(
            isTestnet = false,
            isCustom = true,
            iconUrl = "https://example.com/usdt.png",
            contractAddress = USDT_CONTRACT,
        )
        val status = buildStatus(currency = token, isError = false)

        val result = sut.convert(status) as CurrencyIconState.TokenIcon

        assertThat(result.url).isEqualTo("https://example.com/usdt.png")
        assertThat(result.shouldShowCustomBadge).isTrue()
    }

    // endregion

    // region helpers

    private fun buildCoin(
        isTestnet: Boolean,
        isCustom: Boolean,
        iconUrl: String?,
    ): CryptoCurrency.Coin {
        val network: Network = mockk { every { this@mockk.isTestnet } returns isTestnet }
        val coin: CryptoCurrency.Coin = mockk()
        every { coin.network } returns network
        every { coin.iconUrl } returns iconUrl
        every { coin.isCustom } returns isCustom
        every { coin.networkIconResId } returns NETWORK_ICON_RES_ID
        return coin
    }

    private fun buildToken(
        isTestnet: Boolean,
        isCustom: Boolean,
        iconUrl: String?,
        contractAddress: String,
    ): CryptoCurrency.Token {
        val network: Network = mockk { every { this@mockk.isTestnet } returns isTestnet }
        val token: CryptoCurrency.Token = mockk()
        every { token.network } returns network
        every { token.iconUrl } returns iconUrl
        every { token.isCustom } returns isCustom
        every { token.contractAddress } returns contractAddress
        every { token.networkIconResId } returns NETWORK_ICON_RES_ID
        return token
    }

    private fun buildStatus(currency: CryptoCurrency, isError: Boolean): CryptoCurrencyStatus {
        val value: CryptoCurrencyStatus.Value = mockk { every { this@mockk.isError } returns isError }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    // endregion

    private companion object {
        const val NETWORK_ICON_RES_ID = 1234
        const val USDT_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
}