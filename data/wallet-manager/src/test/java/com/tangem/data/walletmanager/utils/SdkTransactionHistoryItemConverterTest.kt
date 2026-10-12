package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.TxInfo
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
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
        const val RECIPIENT = "0xbc2e11d5605b80e5d4f91d1d6dde2d461126edd8"
    }
}