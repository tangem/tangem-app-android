package com.tangem.features.tangempay.txhistory

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_24
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.cashback
import com.tangem.features.tangempay.collateralTransaction
import com.tangem.features.tangempay.details.impl.R
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

    @ParameterizedTest
    @MethodSource("provideCollateralModels")
    fun `GIVEN collateral type WHEN convert THEN row title, icon and signed amount match the type`(
        model: CollateralModel,
    ) {
        // Arrange
        val converter = TangemPayTxHistoryItemsConverter(txHistoryUiActions = uiActions)
        val collateral = collateralTransaction(type = model.type, amount = model.amount)

        // Act
        val actual = converter.convert(collateral) as TangemPayTransactionState.Content.Collateral

        // Assert
        val expected = TangemPayTransactionState.Content.Collateral(
            id = collateral.id,
            onClick = actual.onClick,
            amount = model.expectedAmount,
            amountColor = actual.amountColor,
            title = model.expectedTitle,
            subtitle = resourceReference(R.string.common_transfer),
            icon = TangemIconUM.Icon(
                imageVector = model.expectedIcon,
                tintReference = (actual.icon as TangemIconUM.Icon).tint,
            ),
            time = actual.time,
        )
        assertThat(actual).isEqualTo(expected)
    }

    data class CashbackModel(
        val cashback: TangemPayTxHistoryItem.Cashback?,
        val expected: TangemPayTransactionCashbackUM?,
    )

    data class CollateralModel(
        val type: TangemPayTxHistoryItem.Type,
        val amount: BigDecimal,
        val expectedTitle: TextReference,
        val expectedAmount: String,
        val expectedIcon: ImageVector,
    )

    private fun provideCollateralModels() = listOf(
        CollateralModel(
            type = TangemPayTxHistoryItem.Type.Deposit,
            amount = BigDecimal("50.00"),
            expectedTitle = resourceReference(R.string.tangem_pay_deposit),
            expectedAmount = "+$50.00",
            expectedIcon = Icons.ic_arrow_down_24,
        ),
        CollateralModel(
            type = TangemPayTxHistoryItem.Type.Withdrawal,
            amount = BigDecimal("-20.00"),
            expectedTitle = resourceReference(R.string.tangem_pay_withdrawal),
            expectedAmount = "-$20.00",
            expectedIcon = Icons.ic_arrow_up_24,
        ),
        CollateralModel(
            type = TangemPayTxHistoryItem.Type.Cashback,
            amount = BigDecimal("19.00"),
            expectedTitle = resourceReference(R.string.tangempay_cashback_title),
            expectedAmount = "+$19.00",
            expectedIcon = Icons.ic_percent_backward_24,
        ),
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