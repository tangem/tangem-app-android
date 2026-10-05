package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.domain.models.network.TxInfo
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SdkTransactionHistoryItemConverterTest {

    private val jointNetwork = MockCryptoCurrencyFactory().ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")
        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }

    private val converter = SdkTransactionHistoryItemConverter(
        smartContractMethods = emptyMap(),
        yieldSupplyAddresses = emptySet(),
        gaslessFeeAddresses = emptySet(),
        jointRefundReceiver = null,
        currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum),
        networkTokens = emptySet(),
        getCachedTokenInfoUseCase = mockk(relaxed = true),
        currencyAddresses = emptyList(),
        txHistoryFeatureToggles = mockk(relaxed = true),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun interactionAddress(model: InteractionModel) {
        // Act
        val actual = converter.convert(item(type = model.type, isOutgoing = model.isOutgoing)).interactionAddressType

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        InteractionModel(
            type = TransactionHistoryItem.TransactionType.Transfer,
            isOutgoing = true,
            expected = TxInfo.InteractionAddressType.User(RECIPIENT),
        ),
        InteractionModel(
            type = TransactionHistoryItem.TransactionType.Transfer,
            isOutgoing = false,
            expected = TxInfo.InteractionAddressType.User(SENDER),
        ),
        // The wallet called a contract: the contract is who it dealt with
        InteractionModel(
            type = TransactionHistoryItem.TransactionType.ContractMethod(id = "0x0779afe6", callData = null),
            isOutgoing = true,
            expected = TxInfo.InteractionAddressType.User(RECIPIENT),
        ),
        // A token arrived inside someone else's contract call: the destination is the wallet, the sender is the
        // counterparty
        InteractionModel(
            type = TransactionHistoryItem.TransactionType.ContractMethod(id = "0x0779afe6", callData = null),
            isOutgoing = false,
            expected = TxInfo.InteractionAddressType.User(SENDER),
        ),
        InteractionModel(
            type = TransactionHistoryItem.TransactionType.ContractMethodName(name = "withdraw", callData = null),
            isOutgoing = false,
            expected = TxInfo.InteractionAddressType.User(SENDER),
        ),
    )

    @Test
    fun `GIVEN a joint token transfer record WHEN convert THEN the row is what the Safe did`() {
        // Arrange — the token's record carries the calldata of the Safe execution that sent it
        val usdt = MockCryptoCurrencyFactory().createToken(Blockchain.Ethereum).copy(network = jointNetwork)
        val jointConverter = SdkTransactionHistoryItemConverter(
            smartContractMethods = mapOf(
                "0x6a761202" to SmartContractMethod(info = "Safe", source = null, name = "execTransaction"),
            ),
            yieldSupplyAddresses = emptySet(),
            gaslessFeeAddresses = emptySet(),
            jointRefundReceiver = null,
            currency = usdt,
            networkTokens = setOf(usdt),
            getCachedTokenInfoUseCase = mockk(relaxed = true),
            currencyAddresses = listOf(Address(SAFE_ADDRESS)),
            txHistoryFeatureToggles = mockk(relaxed = true),
        )
        val item = TransactionHistoryItem(
            txHash = "0xhash",
            timestamp = 1_700_000_000_000L,
            isOutgoing = true,
            destinationType = TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(SAFE_RECIPIENT),
            ),
            sourceType = TransactionHistoryItem.SourceType.Single(SAFE_ADDRESS),
            status = TransactionHistoryItem.TransactionStatus.Confirmed,
            type = TransactionHistoryItem.TransactionType.ContractMethod(
                id = "0x6a761202",
                callData = EXEC_TOKEN_TRANSFER_CALLDATA,
            ),
            amount = Amount(currencySymbol = usdt.symbol, value = BigDecimal.ONE, decimals = usdt.decimals),
            fee = Amount(value = BigDecimal("0.0001"), blockchain = Blockchain.Ethereum),
        )
        val refundCap = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.063191780646394000"), decimals = 18)
        val amount = BigDecimal(1_000_000).movePointLeft(usdt.decimals)

        // Act
        val actual = jointConverter.convert(item)

        // Assert
        assertThat(actual).isEqualTo(
            TxInfo.Single(
                txHash = "0xhash",
                timestampInMillis = 1_700_000_000_000L,
                isOutgoing = true,
                destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(SAFE_RECIPIENT)),
                sourceType = TxInfo.SourceType.Single(SAFE_ADDRESS),
                interactionAddressType = TxInfo.InteractionAddressType.User(SAFE_RECIPIENT),
                status = TxInfo.TransactionStatus.Confirmed,
                type = TxInfo.TransactionType.Joint.Transfer(
                    recipient = SAFE_RECIPIENT,
                    amount = amount,
                    refundCap = refundCap,
                    signaturesCount = 2,
                ),
                amount = amount,
                fee = refundCap,
            ),
        )
    }

    @Test
    fun `GIVEN a joint token transfer refunded in that token WHEN convertList THEN the refund is its fee`() {
        // Arrange — the token's history lists the Safe's two transfers of one execution as two records
        val usdc = MockCryptoCurrencyFactory().createToken(Blockchain.Ethereum).copy(network = jointNetwork)
        val converter = SdkTransactionHistoryItemConverter(
            smartContractMethods = mapOf(
                "0x6a761202" to SmartContractMethod(info = "Safe", source = null, name = "execTransaction"),
            ),
            yieldSupplyAddresses = emptySet(),
            gaslessFeeAddresses = emptySet(),
            jointRefundReceiver = RELAYER,
            currency = usdc,
            networkTokens = setOf(usdc),
            getCachedTokenInfoUseCase = mockk(relaxed = true),
            currencyAddresses = listOf(Address(SAFE_ADDRESS)),
            txHistoryFeatureToggles = mockk { every { isNewTxHistoryEnabled } returns true },
        )

        // Act
        val actual = converter.convertList(
            listOf(
                safeTokenLeg(to = SAFE_RECIPIENT, value = BigDecimal.ONE),
                safeTokenLeg(to = RELAYER, value = REFUND),
            ),
        )

        // Assert
        val row = actual.single() as TxInfo.MultiTransfer
        assertThat(row.amount).isEqualTo(BigDecimal.ONE)
        assertThat(row.isOutgoing).isTrue()
        assertThat(row.destinationType)
            .isEqualTo(TxInfo.DestinationType.Single(TxInfo.AddressType.User(SAFE_RECIPIENT)))
        assertThat(row.gaslessFee?.amount).isEqualTo(REFUND)
    }

    @Test
    fun `GIVEN a Safe transfer and its refund WHEN convertList THEN one outgoing row with the refund as its fee`() {
        // Arrange — Etherscan lists the refund first and keeps the relayer's zero call
        val converter = jointCoinConverter()

        // Act
        val actual = converter.convertList(
            listOf(
                relayerCall(),
                safeCoinLeg(to = RELAYER, value = REFUND),
                safeCoinLeg(to = SAFE_RECIPIENT, value = BigDecimal.TEN),
            ),
        )

        // Assert
        val row = actual.single() as TxInfo.MultiTransfer
        assertThat(row.amount).isEqualTo(BigDecimal.TEN)
        assertThat(row.isOutgoing).isTrue()
        assertThat(row.destinationType)
            .isEqualTo(TxInfo.DestinationType.Single(TxInfo.AddressType.User(SAFE_RECIPIENT)))
        assertThat(row.gaslessFee?.amount).isEqualTo(REFUND)
        assertThat(row.transfers.map { it.type })
            .containsExactly(TxInfo.TransactionType.Transfer, TxInfo.TransactionType.GaslessFee).inOrder()
    }

    @Test
    fun `GIVEN only a Safe refund WHEN convertList THEN a network fee row`() {
        // Arrange — a token operation refunded in the coin moves no coin but the refund
        val converter = jointCoinConverter()

        // Act
        val actual = converter.convertList(listOf(relayerCall(), safeCoinLeg(to = RELAYER, value = REFUND)))

        // Assert
        val row = actual.single() as TxInfo.Single
        assertThat(row.type).isEqualTo(TxInfo.TransactionType.GaslessFee)
        assertThat(row.amount).isEqualTo(REFUND)
        assertThat(row.isOutgoing).isTrue()
    }

    @Test
    fun `GIVEN a failed Safe transfer on Blockbook WHEN convertList THEN the row is failed and the refund is not`() {
        // Arrange — Blockbook gives every internal call the status of the transaction
        val converter = jointCoinConverter()
        val failed = TransactionHistoryItem.TransactionStatus.Failed

        // Act
        val actual = converter.convertList(
            listOf(
                safeCoinLeg(to = SAFE_RECIPIENT, value = BigDecimal.TEN, status = failed),
                safeCoinLeg(to = RELAYER, value = REFUND, status = failed),
            ),
        )

        // Assert
        val row = actual.single() as TxInfo.MultiTransfer
        assertThat(row.status).isEqualTo(TxInfo.TransactionStatus.Failed)
        assertThat(row.gaslessFee?.status).isEqualTo(TxInfo.TransactionStatus.Confirmed)
    }

    @Test
    fun `GIVEN only the relayer's zero call WHEN convertList THEN nothing`() {
        // Arrange — a token operation refunded in the token: no coin moved at all
        val converter = jointCoinConverter()

        // Act
        val actual = converter.convertList(listOf(relayerCall()))

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a personal account paying the relayer address WHEN convertList THEN an ordinary transfer`() {
        // Arrange
        val converter = SdkTransactionHistoryItemConverter(
            smartContractMethods = emptyMap(),
            yieldSupplyAddresses = emptySet(),
            gaslessFeeAddresses = emptySet(),
            jointRefundReceiver = null,
            currency = MockCryptoCurrencyFactory().ethereum,
            networkTokens = emptySet(),
            getCachedTokenInfoUseCase = mockk(relaxed = true),
            currencyAddresses = listOf(Address(SAFE_ADDRESS)),
            txHistoryFeatureToggles = mockk { every { isNewTxHistoryEnabled } returns true },
        )

        // Act
        val actual = converter.convertList(listOf(safeCoinLeg(to = RELAYER, value = REFUND)))

        // Assert
        assertThat((actual.single() as TxInfo.Single).type).isEqualTo(TxInfo.TransactionType.Transfer)
    }

    private fun jointCoinConverter() = SdkTransactionHistoryItemConverter(
        smartContractMethods = mapOf(
            "0x6a761202" to SmartContractMethod(info = "Safe", source = null, name = "execTransaction"),
        ),
        yieldSupplyAddresses = emptySet(),
        gaslessFeeAddresses = emptySet(),
        jointRefundReceiver = RELAYER,
        currency = MockCryptoCurrencyFactory().ethereum.copy(network = jointNetwork),
        networkTokens = emptySet(),
        getCachedTokenInfoUseCase = mockk(relaxed = true),
        currencyAddresses = listOf(Address(SAFE_ADDRESS)),
        txHistoryFeatureToggles = mockk { every { isNewTxHistoryEnabled } returns true },
    )

    /** An internal CALL of the Safe as the SDK reports it: a plain transfer with no fee of its own */
    private fun safeCoinLeg(
        to: String,
        value: BigDecimal,
        status: TransactionHistoryItem.TransactionStatus = TransactionHistoryItem.TransactionStatus.Confirmed,
    ) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 1_700_000_000_000L,
        isOutgoing = true,
        destinationType = TransactionHistoryItem.DestinationType.Single(
            addressType = TransactionHistoryItem.AddressType.User(to),
        ),
        sourceType = TransactionHistoryItem.SourceType.Single(SAFE_ADDRESS),
        status = status,
        type = TransactionHistoryItem.TransactionType.Transfer,
        amount = Amount(value = value, blockchain = Blockchain.Ethereum),
        fee = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
    )

    /** The relayer's `execTransaction` to the Safe, which moves no coin itself */
    private fun relayerCall() = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 1_700_000_000_000L,
        isOutgoing = false,
        destinationType = TransactionHistoryItem.DestinationType.Single(
            addressType = TransactionHistoryItem.AddressType.Contract(SAFE_ADDRESS),
        ),
        sourceType = TransactionHistoryItem.SourceType.Single(RELAYER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.ContractMethodName(
            name = "execTransaction",
            callData = EXEC_TRANSACTION_CALLDATA,
        ),
        amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
        fee = Amount(value = BigDecimal("0.01"), blockchain = Blockchain.Ethereum),
    )

    /** Etherscan's token records carry the method id but no calldata */
    private fun safeTokenLeg(to: String, value: BigDecimal) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 1_700_000_000_000L,
        isOutgoing = true,
        destinationType = TransactionHistoryItem.DestinationType.Single(
            addressType = TransactionHistoryItem.AddressType.User(to),
        ),
        sourceType = TransactionHistoryItem.SourceType.Single(SAFE_ADDRESS),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.ContractMethod(id = "0x6a761202", callData = null),
        amount = Amount(currencySymbol = "USDC", value = value, decimals = 6),
        fee = Amount(value = BigDecimal("0.0001"), blockchain = Blockchain.Ethereum),
    )

    private fun item(type: TransactionHistoryItem.TransactionType, isOutgoing: Boolean) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = isOutgoing,
        destinationType = TransactionHistoryItem.DestinationType.Single(
            addressType = TransactionHistoryItem.AddressType.User(RECIPIENT),
        ),
        sourceType = TransactionHistoryItem.SourceType.Single(SENDER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = type,
        amount = Amount(value = BigDecimal.ONE, blockchain = Blockchain.Ethereum),
        fee = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
    )

    internal data class InteractionModel(
        val type: TransactionHistoryItem.TransactionType,
        val isOutgoing: Boolean,
        val expected: TxInfo.InteractionAddressType,
    )

    private companion object {
        const val SENDER = "0x2c6e7c809ac1adf71c6dd0d67d488d2938eff69a"
        const val SAFE_ADDRESS = "0x4cd2b9a4197dd56e8bde666691a18b2bfa9d3c74"
        val REFUND: BigDecimal = BigDecimal("0.2")
        const val RELAYER = "0x5f2c435df8d00d0cf842a52dd166b1dcd69734c2"
        const val SAFE_RECIPIENT = "0x6dbd0c1957dfe9d85f74b8fe3a355b268a39ec91"

        const val EXEC_TRANSACTION_CALLDATA = "0x" +
            "6a7612020000000000000000000000006dbd0c1957dfe9d85f74b8fe3a355b268a39ec91000000000000000000000000" +
            "00000000000000000000000097c1b80007907d8700000000000000000000000000000000000000000000000000000000" +
            "000001400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000007b0c00000000000000000000000000000000000000000000000000000000" +
            "0001adb000000000000000000000000000000000000000000000000000000067fa8aa71c000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000005f2c435df8d00d0cf842a52dd166b1dc" +
            "d69734c20000000000000000000000000000000000000000000000000000000000000160000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000082d61cf7bb84b6ebac672bb675db53a720283dfc969719a3bafdf85464c8421ce421d32cb9bff75edb566ebb2e" +
            "0996d1212f06f0403dc3fec2d435ff661500d1a61c97dc26b8e9f43bd236d2ddfc32da5c908a1f89064c413edfecdef7" +
            "f9a565397e6acfd0bb175126e0935b6b8275d242a90d075cb76743c035f1410f379fcf7c3c1b00000000000000000000" +
            "0000000000000000000000000000000000000000"
        const val RECIPIENT = "0xbc2e11d5605b80e5d4f91d1d6dde2d461126edd8"

        /** [EXEC_TRANSACTION_CALLDATA] re-encoded around a `transfer(SAFE_RECIPIENT, 1_000_000)` of USDT */
        const val EXEC_TOKEN_TRANSFER_CALLDATA = "0x" +
            "6a761202000000000000000000000000dac17f958d2ee523a2206206994597c13d831ec7000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000001400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000007b0c00000000000000000000000000000000000000000000000000000000" +
            "0001adb000000000000000000000000000000000000000000000000000000067fa8aa71c000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000005f2c435df8d00d0cf842a52dd166b1dc" +
            "d69734c200000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000" +
            "0000000000000000000000000000000000000044a9059cbb0000000000000000000000006dbd0c1957dfe9d85f74b8fe" +
            "3a355b268a39ec9100000000000000000000000000000000000000000000000000000000000f42400000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000821111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111111111111111111111222222222222222222222222222222222222222222222222222222" +
            "2222222222222222222222222222222222222222222222222222222222222222222222222222"
    }
}