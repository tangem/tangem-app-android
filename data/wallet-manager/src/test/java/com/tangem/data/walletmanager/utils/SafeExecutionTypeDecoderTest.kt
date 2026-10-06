package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SafeExecutionTypeDecoderTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val jointNetwork: Network = currencyFactory.ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")
        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }
    private val jointCoin = currencyFactory.ethereum.copy(network = jointNetwork)
    private val jointToken = currencyFactory.createToken(Blockchain.Ethereum).copy(network = jointNetwork)

    @ParameterizedTest
    @ProvideTestModels
    fun isOwnSafeZeroCall(model: OwnSafeZeroCallModel) {
        // Arrange
        val decoder = SafeExecutionTypeDecoder(
            currency = model.currency,
            networkTokens = emptySet(),
            safeAddress = model.safeAddress,
            jointRefundReceiver = RELAYER,
        )

        // Act
        val actual = decoder.isOwnSafeZeroCall(model.item)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        OwnSafeZeroCallModel(currency = jointCoin, safeAddress = SAFE_ADDRESS, item = relayerCall(), expected = true),
        OwnSafeZeroCallModel(currency = jointToken, safeAddress = SAFE_ADDRESS, item = relayerCall(), expected = false),
        OwnSafeZeroCallModel(
            currency = currencyFactory.ethereum,
            safeAddress = SAFE_ADDRESS,
            item = relayerCall(),
            expected = false,
        ),
        OwnSafeZeroCallModel(
            currency = jointCoin,
            safeAddress = OTHER_SAFE_ADDRESS,
            item = relayerCall(),
            expected = false,
        ),
        OwnSafeZeroCallModel(
            currency = jointCoin,
            safeAddress = SAFE_ADDRESS,
            item = relayerCall(value = BigDecimal.ONE),
            expected = false,
        ),
    )

    /** The relayer's `execTransaction` to the Safe, which moves no coin itself */
    private fun relayerCall(value: BigDecimal = BigDecimal.ZERO) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = false,
        destinationType = TransactionHistoryItem.DestinationType.Single(
            addressType = TransactionHistoryItem.AddressType.Contract(SAFE_ADDRESS),
        ),
        sourceType = TransactionHistoryItem.SourceType.Single(RELAYER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.ContractMethodName(name = "execTransaction", callData = null),
        amount = Amount(value = value, blockchain = Blockchain.Ethereum),
        fee = Amount(value = BigDecimal("0.01"), blockchain = Blockchain.Ethereum),
    )

    internal data class OwnSafeZeroCallModel(
        val currency: CryptoCurrency,
        val safeAddress: String,
        val item: TransactionHistoryItem,
        val expected: Boolean,
    )

    private companion object {
        const val SAFE_ADDRESS = "0x4cd2b9a4197dd56e8bde666691a18b2bfa9d3c74"
        const val OTHER_SAFE_ADDRESS = "0x9ac8cebb0ee0efd32c57e9824e5d332e042d04df"
        const val RELAYER = "0x5f2c435df8d00d0cf842a52dd166b1dcd69734c2"
    }
}