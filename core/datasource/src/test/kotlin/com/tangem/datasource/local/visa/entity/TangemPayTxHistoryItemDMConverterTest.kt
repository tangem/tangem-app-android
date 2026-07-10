package com.tangem.datasource.local.visa.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.test.core.ProvideTestModels
import org.joda.time.DateTime
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTxHistoryItemDMConverterTest {

    private val toDMConverter = TangemPayTxHistoryItemToDMConverter()
    private val toDomainConverter = TangemPayTxHistoryItemToDomainConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN every item subtype WHEN converted to DM and back THEN all fields preserved`(
        item: TangemPayTxHistoryItem,
    ) {
        assertRoundTrip(item)
    }

    @ParameterizedTest
    @EnumSource(TangemPayTxHistoryItem.Status::class)
    fun `GIVEN every Spend status WHEN converted to DM and back THEN status preserved`(
        status: TangemPayTxHistoryItem.Status,
    ) {
        assertRoundTrip(spend().copy(status = status))
    }

    @ParameterizedTest
    @EnumSource(TangemPayTxHistoryItem.Type::class)
    fun `GIVEN every Collateral type WHEN converted to DM and back THEN type preserved`(
        type: TangemPayTxHistoryItem.Type,
    ) {
        assertRoundTrip(collateral().copy(type = type))
    }

    private fun assertRoundTrip(item: TangemPayTxHistoryItem) {
        // Act
        val restored = toDomainConverter.convert(toDMConverter.convert(item))

        // Assert
        assertThat(restored).isEqualTo(item)
    }

    private fun provideTestModels() = listOf(spend(), payment(), fee(), collateral())

    private fun spend() = TangemPayTxHistoryItem.Spend(
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
        status = TangemPayTxHistoryItem.Status.COMPLETED,
        enrichedMerchantIconUrl = "https://example.com/icon.png",
        declinedReason = null,
    )

    private fun payment() = TangemPayTxHistoryItem.Payment(
        id = "payment-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("100.00"),
        currency = Currency.getInstance("USD"),
        transactionHash = "0xabc",
    )

    private fun fee() = TangemPayTxHistoryItem.Fee(
        id = "fee-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("0.50"),
        currency = Currency.getInstance("USD"),
        description = "network fee",
    )

    private fun collateral() = TangemPayTxHistoryItem.Collateral(
        id = "collateral-1",
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("250.00"),
        currency = Currency.getInstance("USD"),
        transactionHash = "0xdef",
        type = TangemPayTxHistoryItem.Type.Deposit,
    )

    private companion object {
        const val DATE_MILLIS = 1_700_000_000_000L
    }
}