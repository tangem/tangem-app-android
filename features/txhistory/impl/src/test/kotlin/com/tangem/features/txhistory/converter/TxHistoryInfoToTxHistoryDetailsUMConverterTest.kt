package com.tangem.features.txhistory.converter

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoToTxHistoryDetailsUMConverterTest {

    private val currency = MockCryptoCurrencyFactory().ethereum
    private val copiedAddresses = mutableListOf<String>()
    private val converter = TxHistoryInfoToTxHistoryDetailsUMConverter(
        currency = currency,
        onCopyAddress = copiedAddresses::add,
    )

    @BeforeEach
    fun setUp() {
        // The header subtitle formats the date via DateTimeFormatters -> DateFormat.getBestDateTimePattern,
        // which is an Android stub on the JVM. Mirror the DateTimeFormattersTest mock so convert() runs.
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    // region On-chain (TxInfo)

    @Test
    fun `GIVEN on-chain Swap WHEN convert THEN SingleAsset fallback`() {
        // A two-asset swap always surfaces as ExpressTx.Swap; an on-chain TxInfo of type Swap has no legs,
        // so it falls back to the single amount it does carry rather than an empty two-asset card.
        // Arrange
        val tx = onChain(type = TransactionType.Swap)

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN non-Swap TransactionType WHEN convert THEN SingleAsset`(type: TransactionType) {
        // Act
        val result = converter.convert(onChain(type = type))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
    }

    private fun provideTestModels() = listOf(
        TransactionType.Transfer,
        TransactionType.Approve,
        TransactionType.Operation(name = "Mint NFT"),
        TransactionType.UnknownOperation,
        TransactionType.GaslessFee,
        TransactionType.Staking.Stake,
        TransactionType.Staking.ClaimRewards,
        TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
        TransactionType.YieldSupply.Topup,
        TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
    )

    @Test
    fun `GIVEN incoming confirmed Transfer WHEN convert THEN header has down icon, confirmed status, transferred title`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer)

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.iconRes).isEqualTo(R.drawable.ic_arrow_down_24)
        assertThat(header.status).isEqualTo(TransactionItemUM.Content.Status.Confirmed)
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_transferred))
    }

    @Test
    fun `GIVEN Swap WHEN convert THEN header has exchange icon`() {
        // Arrange
        val tx = onChain(type = TransactionType.Swap)

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.iconRes).isEqualTo(R.drawable.ic_exchange_vertical_24)
    }

    @Test
    fun `GIVEN incoming Transfer WHEN convert THEN amount block has plus sign and not failed`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, isOutgoing = false)

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        assertThat(amountBlock.amount.resolveString()).startsWith("+ ")
        assertThat(amountBlock.isFailed).isFalse()
    }

    @Test
    fun `GIVEN outgoing Transfer WHEN convert THEN amount block has minus sign`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, isOutgoing = true)

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        assertThat(amountBlock.amount.resolveString()).startsWith("- ")
    }

    @Test
    fun `GIVEN zero amount WHEN convert THEN amount block has no sign`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, isOutgoing = true, amount = BigDecimal.ZERO)

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @Test
    fun `GIVEN failed outgoing Transfer WHEN convert THEN amount block is failed and drops the sign`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
        )

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        assertThat(amountBlock.isFailed).isTrue()
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @Test
    fun `GIVEN no interaction address WHEN convert THEN counterparty is null`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, interactionAddressType = null)

        // Act
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Assert
        assertThat(counterparty).isNull()
    }

    @Test
    fun `GIVEN incoming Transfer with User address WHEN convert THEN address-avatar counterparty with From label`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Assert
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.Address(USER_ADDRESS))
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.common_from))
    }

    @Test
    fun `GIVEN outgoing Transfer with User address WHEN convert THEN counterparty has Recipient label`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Assert
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
    }

    @Test
    fun `GIVEN address counterparty WHEN onCopyClick invoked THEN raw address is copied`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Act
        counterparty?.onCopyClick?.invoke()

        // Assert
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
    }

    // endregion

    // region Express (swap / onramp)

    @Test
    fun `GIVEN express swap WHEN convert THEN TwoAssets with exchange icon`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.TwoAssets::class.java)
        assertThat(result.header.iconRes).isEqualTo(R.drawable.ic_exchange_vertical_24)
    }

    @Test
    fun `GIVEN in-progress express swap WHEN convert THEN info status banner with loader`() {
        // Act
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging))
        val banner = (swap as TxHistoryDetailsUM.TwoAssets).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Info,
                title = resourceReference(R.string.express_exchange_status_receiving_active),
                isLoading = true,
            ),
        )
    }

    @Test
    fun `GIVEN failed express swap WHEN convert THEN error status banner with refund subtitle`() {
        // Act
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed))
        val banner = (swap as TxHistoryDetailsUM.TwoAssets).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Error,
                title = resourceReference(R.string.express_exchange_status_failed),
                subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN finished express onramp WHEN convert THEN TwoAssets with success banner`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished)) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.header.iconRes).isEqualTo(R.drawable.ic_tangem_card_24)
        assertThat(result.statusBanner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Success,
                title = resourceReference(R.string.express_exchange_status_exchanged),
                isLoading = false,
            ),
        )
    }

    // endregion

    private fun onChain(
        type: TransactionType,
        isOutgoing: Boolean = false,
        status: TxInfo.TransactionStatus = TxInfo.TransactionStatus.Confirmed,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = null,
    ): OnChainTx.BSDK = OnChainTx.BSDK(
        TxInfo(
            txHash = TX_HASH,
            timestampInMillis = TIMESTAMP,
            isOutgoing = isOutgoing,
            destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
            sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
            interactionAddressType = interactionAddressType,
            status = status,
            type = type,
            amount = amount,
        ),
    )

    private fun expressSwap(status: ExpressExchangeStatus): ExpressTx.Swap = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "swap-1",
            status = status,
            createdAtMillis = TIMESTAMP,
            provider = null,
            payinHash = null,
            payoutHash = null,
            fromAsset = expressAsset(networkId = "ethereum", amount = BigDecimal("1.5"), decimals = 18),
            toAsset = expressAsset(networkId = "bitcoin", amount = BigDecimal("0.001"), decimals = 8),
        ),
        isOutgoing = true,
        txInfo = null,
    )

    private fun expressOnramp(status: ExpressOnrampStatus): ExpressTx.Onramp = ExpressTx.Onramp(
        tx = OnrampTransaction(
            txId = "onramp-1",
            status = status,
            createdAtMillis = TIMESTAMP,
            provider = null,
            payoutHash = null,
            fromFiat = Amount(
                currencySymbol = "SEK",
                value = BigDecimal("100"),
                decimals = 2,
                type = AmountType.FiatType(code = "SEK"),
            ),
            toAsset = expressAsset(networkId = "bitcoin", amount = BigDecimal("0.006"), decimals = 8),
        ),
        txInfo = null,
    )

    private fun expressAsset(networkId: String, amount: BigDecimal, decimals: Int): ExpressTransactionAsset =
        ExpressTransactionAsset(
            id = ExpressAssetId(networkId = networkId, contractAddress = "0"),
            amount = amount,
            decimals = decimals,
        )

    private fun TextReference.resolveString(): String = (this as TextReference.Str).value

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
        const val VALIDATOR_ADDRESS = "0xvalidator"
    }
}