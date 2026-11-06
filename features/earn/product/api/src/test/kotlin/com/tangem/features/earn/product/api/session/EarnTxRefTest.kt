package com.tangem.features.earn.product.api.session

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.test.core.ProvideTestModels
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTxRefTest {

    private val json = Json

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN tx ref WHEN encoded and decoded THEN equal ref is restored`(model: RoundTripModel) {
        // Arrange
        val encoded = json.encodeToString(EarnTxRef.serializer(), model.ref)

        // Act
        val actual = json.decodeFromString(EarnTxRef.serializer(), encoded)

        // Assert
        assertThat(actual).isEqualTo(model.ref)
    }

    @Test
    fun `GIVEN tx ref WHEN encoded THEN format carries its version`() {
        // Arrange
        val ref = createRef(version = 1)
        val encoded = json.encodeToString(EarnTxRef.serializer(), ref)

        // Act
        val actual = Json.parseToJsonElement(encoded).jsonObject["version"]

        // Assert
        assertThat(actual).isEqualTo(JsonPrimitive(1))
    }

    private fun provideTestModels() = listOf(
        RoundTripModel(name = "deposit with analytics", ref = createRef()),
        RoundTripModel(name = "empty analytics", ref = createRef(analytics = EarnTxAnalytics())),
        RoundTripModel(name = "vault product", ref = createRef(productId = EarnProductId("vault"))),
        RoundTripModel(
            name = "version 2 with nanosecond timestamp",
            ref = createRef(version = 2, createdAt = Instant.parse("2030-01-01T00:00:00.123456789Z")),
        ),
    )

    internal data class RoundTripModel(val name: String, val ref: EarnTxRef) {
        override fun toString(): String = name
    }

    private fun createRef(
        version: Int = 1,
        productId: EarnProductId = EarnProductId("lending"),
        id: String = "fake-deposit",
        userWalletId: UserWalletId = UserWalletId("0123456789abcdef"),
        currencyId: CryptoCurrency.ID = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum"),
        createdAt: Instant = Instant.parse("2030-01-01T00:00:00Z"),
        analytics: EarnTxAnalytics = EarnTxAnalytics(mapOf("action" to "deposit")),
    ) = EarnTxRef(
        version = version,
        productId = productId,
        id = id,
        userWalletId = userWalletId,
        currencyId = currencyId,
        createdAt = createdAt,
        analytics = analytics,
    )
}