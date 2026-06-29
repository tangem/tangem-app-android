package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SdkTransactionTypeConverterTest {

    private val converter = SdkTransactionTypeConverter(
        smartContractMethods = mapOf(
            GASLESS_METHOD_ID to SmartContractMethod(info = null, source = null, name = "gaslessTransaction"),
        ),
        yieldSupplyAddresses = emptySet(),
        gaslessFeeAddresses = setOf(FEE_RECIPIENT),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(gaslessItem(model.destination))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            destination = singleUser(RECIPIENT),
            expected = TxInfo.TransactionType.UnknownOperation,
        ),
        ConvertModel(
            destination = singleUser(FEE_RECIPIENT),
            expected = TxInfo.TransactionType.GaslessFee,
        ),
        ConvertModel(
            destination = singleUser(FEE_RECIPIENT.uppercase()),
            expected = TxInfo.TransactionType.GaslessFee,
        ),
        ConvertModel(
            destination = TransactionHistoryItem.DestinationType.Multiple(
                addressTypes = listOf(
                    TransactionHistoryItem.AddressType.User(RECIPIENT),
                    TransactionHistoryItem.AddressType.User(FEE_RECIPIENT),
                ),
            ),
            expected = TxInfo.TransactionType.UnknownOperation,
        ),
    )

    private fun gaslessItem(destination: TransactionHistoryItem.DestinationType) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = true,
        destinationType = destination,
        sourceType = TransactionHistoryItem.SourceType.Single(SENDER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.ContractMethod(id = GASLESS_METHOD_ID),
        amount = mockk(),
        fee = mockk(),
    )

    private fun singleUser(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.User(address),
    )

    internal data class ConvertModel(
        val destination: TransactionHistoryItem.DestinationType,
        val expected: TxInfo.TransactionType,
    )

    private companion object {
        const val GASLESS_METHOD_ID = "0x6234d42b"
        const val SENDER = "0x9ffd974772bda94d288240c1b22f367ce75ccd7f"
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val FEE_RECIPIENT = "0x1111111111111111111111111111111111111111"
    }
}