package com.tangem.feature.swap.domain.fee

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [SwapFeeFactory] ([REDACTED_TASK_KEY] — Phase 3).
 *
 * Verifies the bucket → [com.tangem.blockchain.common.transaction.Fee] mapping rules used by
 * `SwapInteractorImpl.loadSwapFee` to assemble a `SwapFee` from a raw `TransactionFeeResult`.
 *
 * Golden mapping table — must match `FeeItemConverter` in send-v2:
 *
 * | TransactionFee shape   | FeeBucket    | Selected Fee                            |
 * |------------------------|--------------|-----------------------------------------|
 * | Single(normal)         | MARKET       | normal                                  |
 * | Single(normal)         | SLOW         | normal (degraded — no minimum)          |
 * | Single(normal)         | FAST         | normal (degraded — no priority)         |
 * | Choosable(min/n/p)     | SLOW         | minimum                                 |
 * | Choosable(min/n/p)     | MARKET       | normal                                  |
 * | Choosable(min/n/p)     | FAST         | priority                                |
 * | Choosable(min/n/p)     | SUGGESTED    | normal (caller overrides if applicable) |
 * | Choosable(min/n/p)     | CUSTOM       | normal (caller overrides)               |
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapFeeFactoryTest {

    private val nativeFeeTokenStatus: CryptoCurrencyStatus = mockk(relaxed = true)

    // -------------------------------------------------------------------------
    // TransactionFee.Single
    // -------------------------------------------------------------------------

    @Test
    fun `fromLoaded with Single picks the normal fee for MARKET bucket`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.MARKET,
        )

        assertThat(result.fee).isEqualTo(singleFee.normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.MARKET)
        assertThat(result.otherNativeFee).isEqualTo(BigDecimal.ZERO)
        assertThat(result.selectedFeeToken).isSameInstanceAs(nativeFeeTokenStatus)
    }

    @Test
    fun `fromLoaded with Single degrades SLOW bucket to normal fee`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.SLOW,
        )

        assertThat(result.fee).isEqualTo(singleFee.normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.SLOW)
    }

    @Test
    fun `fromLoaded with Single degrades FAST bucket to normal fee`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.FAST,
        )

        assertThat(result.fee).isEqualTo(singleFee.normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.FAST)
    }

    // -------------------------------------------------------------------------
    // TransactionFee.Choosable
    // -------------------------------------------------------------------------

    @Test
    fun `fromLoaded with Choosable picks minimum fee for SLOW bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.SLOW,
        )

        assertThat(result.fee).isEqualTo(slow)
    }

    @Test
    fun `fromLoaded with Choosable picks normal fee for MARKET bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.MARKET,
        )

        assertThat(result.fee).isEqualTo(normal)
    }

    @Test
    fun `fromLoaded with Choosable picks priority fee for FAST bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.FAST,
        )

        assertThat(result.fee).isEqualTo(fast)
    }

    @Test
    fun `fromLoaded with Choosable falls back to normal fee for SUGGESTED bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.SUGGESTED,
        )

        assertThat(result.fee).isEqualTo(normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.SUGGESTED)
    }

    @Test
    fun `fromLoaded with Choosable falls back to normal fee for CUSTOM bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.CUSTOM,
        )

        assertThat(result.fee).isEqualTo(normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.CUSTOM)
    }

    // -------------------------------------------------------------------------
    // LoadedExtended (gasless / token fee)
    // -------------------------------------------------------------------------

    @Test
    fun `fromLoadedExtended picks normal fee from transactionFeeExtended for MARKET`() {
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val txFee = TransactionFee.Single(normal = rawFee)
        val extended = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns txFee
        }

        val result = SwapFeeFactory.fromLoadedExtended(
            transactionFeeResult = TransactionFeeResult.LoadedExtended(extended),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.MARKET,
        )

        assertThat(result.fee).isEqualTo(rawFee)
        assertThat(result.transactionFeeResult).isInstanceOf(TransactionFeeResult.LoadedExtended::class.java)
    }

    // -------------------------------------------------------------------------
    // otherNativeFee propagation
    // -------------------------------------------------------------------------

    @Test
    fun `otherNativeFee is propagated verbatim into SwapFee`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val bridgeFee = BigDecimal("0.5")

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            otherNativeFee = bridgeFee,
        )

        assertThat(result.otherNativeFee).isEquivalentAccordingToCompareTo(bridgeFee)
    }

    @Test
    fun `default otherNativeFee is ZERO`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
        )

        assertThat(result.otherNativeFee).isEqualTo(BigDecimal.ZERO)
    }

    // -------------------------------------------------------------------------
    // [REDACTED_TASK_KEY] — bridge-fee folding
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN native coin fee WHEN fromLoaded with Single THEN otherNativeFee folded into fee amount`() {
        // Arrange
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val bridgeFee = BigDecimal("0.5")

        // Act
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = coinFeeTokenStatus(),
            otherNativeFee = bridgeFee,
        )

        // Assert — selected fee and the underlying tier both carry gas + bridge
        assertThat(result.fee.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.502"))
        val folded = (result.transactionFeeResult as TransactionFeeResult.Loaded).fee as TransactionFee.Single
        assertThat(folded.normal.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.502"))
        // otherNativeFee retained as the included portion (non-additive downstream)
        assertThat(result.otherNativeFee).isEquivalentAccordingToCompareTo(bridgeFee)
    }

    @Test
    fun `GIVEN native coin fee WHEN fromLoaded with Choosable THEN all tiers folded`() {
        // Arrange
        val choosable = TransactionFee.Choosable(
            minimum = ethLegacyFee(BigDecimal("0.001")),
            normal = ethLegacyFee(BigDecimal("0.002")),
            priority = ethLegacyFee(BigDecimal("0.003")),
        )

        // Act
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = coinFeeTokenStatus(),
            otherNativeFee = BigDecimal("0.5"),
            feeBucket = FeeBucket.MARKET,
        )

        // Assert
        val folded = (result.transactionFeeResult as TransactionFeeResult.Loaded).fee as TransactionFee.Choosable
        assertThat(folded.minimum.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.501"))
        assertThat(folded.normal.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.502"))
        assertThat(folded.priority.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.503"))
        // Selected MARKET fee = folded normal
        assertThat(result.fee.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.502"))
    }

    @Test
    fun `GIVEN token fee token WHEN fromLoaded with nonzero otherNativeFee THEN not folded`() {
        // Arrange — fee paid in a token: a native bridge fee must not be summed into it
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))

        // Act
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = tokenFeeTokenStatus(),
            otherNativeFee = BigDecimal("0.5"),
        )

        // Assert — fee unchanged, otherNativeFee retained separately
        assertThat(result.fee.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.002"))
        assertThat(result.otherNativeFee).isEquivalentAccordingToCompareTo(BigDecimal("0.5"))
    }

    @Test
    fun `GIVEN TokenCurrency fee subtype WHEN fold attempted THEN returned unchanged`() {
        // Arrange — native coin fee token but a token-denominated Fee subtype: must stay unchanged
        val singleFee = TransactionFee.Single(normal = ethTokenCurrencyFee(BigDecimal("0.002")))

        // Act
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = coinFeeTokenStatus(),
            otherNativeFee = BigDecimal("0.5"),
        )

        // Assert
        assertThat(result.fee.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.002"))
    }

    @Test
    fun `GIVEN zero otherNativeFee WHEN fromLoaded THEN result identity preserved`() {
        // Arrange
        val loaded = TransactionFeeResult.Loaded(TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002"))))

        // Act
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = loaded,
            selectedFeeToken = coinFeeTokenStatus(),
            otherNativeFee = BigDecimal.ZERO,
        )

        // Assert — no rebuild when nothing to fold
        assertThat(result.transactionFeeResult).isSameInstanceAs(loaded)
    }

    @Test
    fun `GIVEN gasless extended fee WHEN fromLoadedExtended with nonzero otherNativeFee THEN not folded`() {
        // Arrange
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val extended = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns TransactionFee.Single(normal = rawFee)
        }

        // Act
        val result = SwapFeeFactory.fromLoadedExtended(
            transactionFeeResult = TransactionFeeResult.LoadedExtended(extended),
            selectedFeeToken = coinFeeTokenStatus(),
            otherNativeFee = BigDecimal("0.5"),
        )

        // Assert — gasless (token-denominated) fee is never folded; otherNativeFee kept separate
        assertThat(result.fee.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.002"))
        assertThat(result.otherNativeFee).isEquivalentAccordingToCompareTo(BigDecimal("0.5"))
    }

    // -------------------------------------------------------------------------
    // from() generic dispatcher
    // -------------------------------------------------------------------------

    @Test
    fun `from dispatches Loaded to fromLoaded`() {
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val transactionFeeResult = TransactionFeeResult.Loaded(TransactionFee.Single(normal = rawFee))

        val result = SwapFeeFactory.from(
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = nativeFeeTokenStatus,
        )

        assertThat(result.fee).isEqualTo(rawFee)
        assertThat(result.transactionFeeResult).isSameInstanceAs(transactionFeeResult)
    }

    @Test
    fun `from dispatches LoadedExtended to fromLoadedExtended`() {
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val txFee = TransactionFee.Single(normal = rawFee)
        val extended = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns txFee
        }
        val transactionFeeResult = TransactionFeeResult.LoadedExtended(extended)

        val result = SwapFeeFactory.from(
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = nativeFeeTokenStatus,
        )

        assertThat(result.fee).isEqualTo(rawFee)
        assertThat(result.transactionFeeResult).isSameInstanceAs(transactionFeeResult)
    }

    // -------------------------------------------------------------------------
    // FeeBucket.toAnalyticsName labels
    // -------------------------------------------------------------------------

    @Test
    fun `FeeBucket toAnalyticsName returns labels compatible with legacy FeeType`() {
        // SLOW didn't exist in the legacy FeeType; new label is "Min".
        assertThat(FeeBucket.SLOW.toAnalyticsName()).isEqualTo("Min")
        // MARKET corresponds to legacy FeeType.NORMAL.getNameForAnalytics() == "Normal".
        assertThat(FeeBucket.MARKET.toAnalyticsName()).isEqualTo("Normal")
        // FAST corresponds to legacy FeeType.PRIORITY.getNameForAnalytics() == "Max".
        assertThat(FeeBucket.FAST.toAnalyticsName()).isEqualTo("Max")
        assertThat(FeeBucket.SUGGESTED.toAnalyticsName()).isEqualTo("Suggested")
        assertThat(FeeBucket.CUSTOM.toAnalyticsName()).isEqualTo("Custom")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun ethLegacyFee(value: BigDecimal): Fee.Ethereum.Legacy = Fee.Ethereum.Legacy(
        amount = Amount(currencySymbol = "ETH", value = value, decimals = 18),
        gasLimit = BigInteger.valueOf(100_000),
        gasPrice = BigInteger.valueOf(20_000_000_000),
    )

    private fun ethTokenCurrencyFee(value: BigDecimal): Fee.Ethereum.TokenCurrency = Fee.Ethereum.TokenCurrency(
        amount = Amount(currencySymbol = "USDT", value = value, decimals = 6),
        gasLimit = BigInteger.valueOf(100_000),
        coinPriceInToken = BigInteger.ONE,
        feeTransferGasLimit = BigInteger.valueOf(21_000),
        baseGas = BigInteger.ZERO,
    )

    /** A fee-token status whose currency really is a [CryptoCurrency.Coin] (so folding applies). */
    private fun coinFeeTokenStatus(): CryptoCurrencyStatus = mockk(relaxed = true) {
        io.mockk.every { currency } returns mockk<CryptoCurrency.Coin>(relaxed = true)
    }

    /** A fee-token status whose currency is a [CryptoCurrency.Token] (so folding is skipped). */
    private fun tokenFeeTokenStatus(): CryptoCurrencyStatus = mockk(relaxed = true) {
        io.mockk.every { currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
    }
}