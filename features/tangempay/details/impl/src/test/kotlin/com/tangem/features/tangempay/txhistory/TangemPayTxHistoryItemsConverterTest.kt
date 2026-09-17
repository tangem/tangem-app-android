package com.tangem.features.tangempay.txhistory

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.cashback
import com.tangem.features.tangempay.spendTransaction
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTxHistoryItemsConverterTest {

    private val defaultLocale = Locale.getDefault()
    private val uiActions = mockk<TangemPayTxHistoryUiActions>(relaxed = true)

    @BeforeEach
    fun setup() {
        // Cashback amount is formatted via the locale-sensitive fiat formatter; pin it for stable assertions.
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN spend cashback WHEN convert THEN badge matches expected`(model: CashbackModel) {
        // Arrange
        val converter = TangemPayTxHistoryItemsConverter(txHistoryUiActions = uiActions)

        // Act
        val result = converter.convert(spendTransaction(cashback = model.cashback))

        // Assert
        assertThat((result as TangemPayTransactionState.Content.Spend).cashback).isEqualTo(model.expected)
    }

    data class CashbackModel(
        val cashback: TangemPayTxHistoryItem.Cashback?,
        val expected: TangemPayTransactionCashbackUM?,
    )

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        CashbackModel(cashback = null, expected = null),
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.CONFIRMED,
                amount = BigDecimal("5.00"),
            ),
            expected = TangemPayTransactionCashbackUM(
                amount = "+$5.00",
                style = TangemPayTransactionCashbackUM.Style.Confirmed,
            ),
        ),
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.ESTIMATED,
                amount = BigDecimal("5.00"),
            ),
            expected = TangemPayTransactionCashbackUM(
                amount = "+$5.00",
                style = TangemPayTransactionCashbackUM.Style.Estimated,
            ),
        ),
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.CONFIRMED,
                amount = BigDecimal("-5.00"),
            ),
            expected = TangemPayTransactionCashbackUM(
                amount = "-$5.00",
                style = TangemPayTransactionCashbackUM.Style.Confirmed,
            ),
        ),
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.ESTIMATED,
                amount = BigDecimal("-5.00"),
            ),
            expected = TangemPayTransactionCashbackUM(
                amount = "-$5.00",
                style = TangemPayTransactionCashbackUM.Style.Estimated,
            ),
        ),
        // Excluded — no cashback earned — hidden even with a non-zero amount.
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.EXCLUDED,
                amount = BigDecimal("5.00"),
            ),
            expected = null,
        ),
        // Zero amount — hidden.
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.CONFIRMED,
                amount = BigDecimal("0.00"),
            ),
            expected = null,
        ),
        // Unknown status — hidden defensively.
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.UNKNOWN,
                amount = BigDecimal("5.00"),
            ),
            expected = null,
        ),
        // Awaiting calculation — amount/currency are null — no badge.
        CashbackModel(
            cashback = cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.AWAITING_CALCULATION,
                amount = null,
                currency = null,
            ),
            expected = null,
        ),
    )
}