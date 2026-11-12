package com.tangem.data.onramp.legacy

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.onramp.repositories.LegacyTopUpRepository
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.security.MessageDigest

internal class MercuryoTopUpRepositoryTest {

    @AfterEach
    fun tearDown() {
        unmockkConstructor(Uri.Builder::class)
    }

    @ParameterizedTest
    @CsvSource("ethereum,ETHEREUM", "unknown,")
    fun `GIVEN network WHEN top up requested THEN signed widget parameters match provider contract`(
        networkId: String,
        expectedNetwork: String?,
    ) = runTest {
        // Arrange
        val config = mockk<GrowEnvironmentConfig> {
            every { mercuryoWidgetId } returns "widget"
            every { mercuryoSecret } returns "secret"
        }
        val currency = mockk<CryptoCurrency.Coin> {
            every { symbol } returns "ETH"
            every { network.id } returns Network.ID(networkId, Network.DerivationPath.None)
        }
        val result = mockk<Uri>()
        every { result.toString() } returns "widget-url"
        val builder = mockk<Uri.Builder>()
        val parameters = mutableMapOf<String, String?>()
        mockkConstructor(Uri.Builder::class)
        every { anyConstructed<Uri.Builder>().scheme("https") } returns builder
        every { builder.authority("exchange.mercuryo.io") } returns builder
        every { builder.appendQueryParameter(any(), any()) } answers {
            parameters[firstArg()] = secondArg()
            builder
        }
        every { builder.build() } returns result
        val repository = MercuryoTopUpRepository(config, TestingCoroutineDispatcherProvider())
        val signature = MessageDigest.getInstance("SHA-512").digest("addresssecret".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val expected = mutableMapOf(
            "widget_id" to "widget", "type" to "buy", "currency" to "ETH", "address" to "address",
            "signature" to signature, "fix_currency" to "true", "redirect_url" to LegacyTopUpRepository.SUCCESS_URL,
        )
        if (expectedNetwork != null) expected["network"] = expectedNetwork
        // Act
        val actual = repository.getTopUpUrl(currency, "address")
        // Assert
        assertThat(actual).isEqualTo("widget-url")
        assertThat(parameters).containsExactlyEntriesIn(expected)
        verify(exactly = 1) { builder.authority("exchange.mercuryo.io") }
    }
}