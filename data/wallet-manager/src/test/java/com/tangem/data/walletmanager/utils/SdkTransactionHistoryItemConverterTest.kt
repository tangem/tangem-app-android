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
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SdkTransactionHistoryItemConverterTest {

    private val converter = SdkTransactionHistoryItemConverter(
        smartContractMethods = emptyMap(),
        yieldSupplyAddresses = emptySet(),
        gaslessFeeAddresses = emptySet(),
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
    fun `GIVEN joint execution record WHEN convert THEN the row is what the Safe did`() {
        // Arrange — the provider record is the relayer's zero-value call to the Safe
        val jointNetwork = MockCryptoCurrencyFactory().ethereum.network.let { network ->
            val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")
            network.copy(
                id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
                derivationPath = derivationPath,
            )
        }
        val jointConverter = SdkTransactionHistoryItemConverter(
            smartContractMethods = mapOf(
                "0x6a761202" to SmartContractMethod(info = "Safe", source = null, name = "execTransaction"),
            ),
            yieldSupplyAddresses = emptySet(),
            gaslessFeeAddresses = emptySet(),
            currency = MockCryptoCurrencyFactory().ethereum.copy(network = jointNetwork),
            networkTokens = emptySet(),
            getCachedTokenInfoUseCase = mockk(relaxed = true),
            currencyAddresses = listOf(Address(SAFE_ADDRESS)),
            txHistoryFeatureToggles = mockk(relaxed = true),
        )
        val item = TransactionHistoryItem(
            txHash = "0xhash",
            timestamp = 1_700_000_000_000L,
            isOutgoing = false,
            destinationType = TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(SAFE_ADDRESS),
            ),
            sourceType = TransactionHistoryItem.SourceType.Single(RELAYER),
            status = TransactionHistoryItem.TransactionStatus.Confirmed,
            type = TransactionHistoryItem.TransactionType.ContractMethod(
                id = "0x6a761202",
                callData = EXEC_TRANSACTION_CALLDATA,
            ),
            amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
            fee = Amount(value = BigDecimal("0.0001"), blockchain = Blockchain.Ethereum),
        )
        val refundCap = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.063191780646394000"), decimals = 18)
        val amount = BigDecimal("10.935223680498695559")

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
                    refundCapWei = BigDecimal("63191780646394000"),
                    signaturesCount = 2,
                ),
                amount = amount,
                fee = refundCap,
            ),
        )
    }

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
    }
}