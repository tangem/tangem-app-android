package com.tangem.features.tangempay.txhistory.details

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.cashback
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.paymentTransaction
import com.tangem.features.tangempay.spendTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

private typealias Status = TangemPayTxHistoryItem.Cashback.Status
private typealias Reason = TangemPayTxHistoryItem.Cashback.ExclusionReason

/** Covers the transaction-detail "Cashback" row built by [TangemPayCashbackDetailUmConverter]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCashbackDetailUmConverterTest {

    private val defaultLocale = Locale.getDefault()

    @BeforeEach
    fun setup() {
        // Amount is formatted via the locale-sensitive fiat formatter; pin it for stable assertions.
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @MethodSource("provideLoadedModels")
    fun `GIVEN loaded cashback WHEN convert THEN row matches expected`(model: CashbackRowModel) {
        // Act
        val result = convert(cashback = model.cashback, loadState = TransactionLoadState.Loaded)

        // Assert
        assertThat(result).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN fetch in flight WHEN convert THEN row is Loading`() {
        val result = convert(cashback = null, loadState = TransactionLoadState.Loading)

        assertThat(result).isEqualTo(CashbackDetailUM.Loading)
    }

    @Test
    fun `GIVEN fetch failed WHEN convert THEN row is Error`() {
        val result = convert(cashback = null, loadState = TransactionLoadState.Error)

        assertThat(result).isInstanceOf(CashbackDetailUM.Error::class.java)
    }

    @Test
    fun `GIVEN cashback feature disabled WHEN convert THEN row is hidden`() {
        val result = convert(cashback = cashback(), loadState = TransactionLoadState.Loaded, isCashbackEnabled = false)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN non-spend transaction WHEN convert THEN row is hidden`() {
        val result = convert(
            transaction = paymentTransaction(),
            cashback = cashback(),
            loadState = TransactionLoadState.Loaded,
        )

        assertThat(result).isNull()
    }

    private fun convert(
        transaction: TangemPayTxHistoryItem = spendTransaction(),
        cashback: TangemPayTxHistoryItem.Cashback?,
        loadState: TransactionLoadState,
        isCashbackEnabled: Boolean = true,
    ): CashbackDetailUM? = TangemPayCashbackDetailUmConverter.convert(
        transaction = transaction,
        cashback = cashback,
        loadState = loadState,
        isCashbackEnabled = isCashbackEnabled,
        onRefreshClick = {},
    )

    data class CashbackRowModel(val cashback: TangemPayTxHistoryItem.Cashback?, val expected: CashbackDetailUM?)

    @Suppress("LongMethod")
    private fun provideLoadedModels() = listOf(
        CashbackRowModel(
            cashback = cashback(status = Status.CONFIRMED, amount = BigDecimal("0.63")),
            expected = CashbackDetailUM.Content(value = stringReference("+\$0.63"), subvalue = null),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.ESTIMATED, amount = BigDecimal("1.70")),
            expected = CashbackDetailUM.Content(value = stringReference("+\$1.70"), subvalue = null),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.CONFIRMED, amount = BigDecimal("-0.80")),
            expected = CashbackDetailUM.Content(
                value = stringReference("-\$0.80"),
                subvalue = resourceReference(R.string.tangem_pay_transaction_details_cashback_refund),
            ),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.CONFIRMED, amount = BigDecimal("3.00"), isCapTrimmed = true),
            expected = CashbackDetailUM.Content(
                value = stringReference("+\$3.00"),
                subvalue = resourceReference(R.string.tangem_pay_transaction_details_cashback_cap_reached),
            ),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.EXCLUDED, amount = ZERO, exclusionReason = Reason.MCC_EXCLUDED),
            expected = excluded(R.string.tangem_pay_transaction_details_cashback_mcc_excluded),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.EXCLUDED, amount = ZERO, exclusionReason = Reason.MONTHLY_CAP_REACHED),
            expected = excluded(R.string.tangem_pay_transaction_details_cashback_cap_reached),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.EXCLUDED, amount = ZERO, exclusionReason = Reason.MERCHANT_COUNTRY_EXCLUDED),
            expected = excluded(R.string.tangem_pay_transaction_details_cashback_region_excluded),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.EXCLUDED, amount = ZERO, exclusionReason = Reason.BELOW_MIN),
            expected = CashbackDetailUM.Content(
                value = resourceReference(R.string.tangem_pay_transaction_details_cashback_none),
                subvalue = stringReference("Below minimum"),
            ),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.EXCLUDED, amount = ZERO),
            expected = CashbackDetailUM.Content(
                value = resourceReference(R.string.tangem_pay_transaction_details_cashback_none),
                subvalue = null,
            ),
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.AWAITING_CALCULATION, amount = null, currency = null),
            expected = CashbackDetailUM.AwaitingCalculation,
        ),
        CashbackRowModel(
            cashback = cashback(status = Status.UNKNOWN, amount = null, currency = null),
            expected = null,
        ),
        CashbackRowModel(cashback = null, expected = null),
    )

    private fun excluded(subvalueRes: Int) = CashbackDetailUM.Content(
        value = resourceReference(R.string.tangem_pay_transaction_details_cashback_none),
        subvalue = resourceReference(subvalueRes),
    )

    private companion object {
        val ZERO: BigDecimal = BigDecimal("0.00")
    }
}