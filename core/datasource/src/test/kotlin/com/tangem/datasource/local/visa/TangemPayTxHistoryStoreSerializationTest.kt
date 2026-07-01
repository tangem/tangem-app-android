package com.tangem.datasource.local.visa

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemDM
import com.tangem.datasource.utils.KotlinxDataStoreSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTxHistoryStoreSerializationTest {

    private val json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" }

    private val serializer = MapSerializer(
        keySerializer = String.serializer(),
        valueSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = ListSerializer(TangemPayTxHistoryItemDM.serializer()),
        ),
    )

    @Test
    fun `GIVEN all tx history item types WHEN serialized and deserialized THEN value is preserved`() {
        // Arrange
        val original: Map<String, Map<String, List<TangemPayTxHistoryItemDM>>> = mapOf(
            "wallet-1" to mapOf(
                "initial_cursor_key" to listOf(spend(), payment(), fee(), collateral()),
            ),
        )

        // Act
        val restored = json.decodeFromString(serializer, json.encodeToString(serializer, original))

        // Assert
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `GIVEN Collateral with type field WHEN serialized and deserialized THEN discriminator does not clash`() {
        // Arrange
        val original: Map<String, Map<String, List<TangemPayTxHistoryItemDM>>> = mapOf(
            "wallet-1" to mapOf("cursor" to listOf(collateral())),
        )

        // Act
        val restored = json.decodeFromString(serializer, json.encodeToString(serializer, original))

        // Assert
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `GIVEN tx history item WHEN serialized THEN uses stable SerialName under non-clashing discriminator`() {
        // Arrange
        val original: Map<String, Map<String, List<TangemPayTxHistoryItemDM>>> = mapOf(
            "wallet-1" to mapOf("cursor" to listOf(collateral())),
        )

        // Act
        val encoded = json.encodeToString(serializer, original)

        // Assert
        assertThat(encoded).contains("\"__type\":\"collateral\"")
    }

    private fun spend() = TangemPayTxHistoryItemDM.Spend(
        id = "spend-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("12.34"),
        currency = Currency.getInstance("USD"),
        authorizedAmount = BigDecimal("12.34"),
        localAmount = BigDecimal("11.00"),
        localCurrency = Currency.getInstance("EUR"),
        enrichedMerchantName = "Coffee Co",
        merchantName = "COFFEE CO",
        enrichedMerchantCategory = "Food",
        merchantCategoryCode = "5814",
        merchantCategory = "restaurants",
        status = TangemPayTxHistoryItemDM.Status.COMPLETED,
        enrichedMerchantIconUrl = "https://example.com/icon.png",
        declinedReason = null,
    )

    private fun payment() = TangemPayTxHistoryItemDM.Payment(
        id = "payment-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("100.00"),
        currency = Currency.getInstance("USD"),
        transactionHash = "0xabc",
    )

    private fun fee() = TangemPayTxHistoryItemDM.Fee(
        id = "fee-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("0.50"),
        currency = Currency.getInstance("USD"),
        description = "network fee",
    )

    private fun collateral() = TangemPayTxHistoryItemDM.Collateral(
        id = "collateral-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("250.00"),
        currency = Currency.getInstance("USD"),
        transactionHash = "0xdef",
        type = TangemPayTxHistoryItemDM.Type.Deposit,
    )

    private companion object {
        const val DATE_MILLIS = 1_700_000_000_000L
    }
}