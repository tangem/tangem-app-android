package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SdkTransactionTypeConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Gasless {

        private val converter = createConverter(currency = currencyFactory.createCoin(Blockchain.Ethereum))

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: GaslessModel) {
            // Act
            val actual = converter.convert(item(destination = model.destination, methodId = model.methodId))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GaslessModel(
                destination = singleUser(RECIPIENT),
                expected = TxInfo.TransactionType.UnknownOperation,
            ),
            GaslessModel(
                destination = singleUser(FEE_RECIPIENT),
                expected = TxInfo.TransactionType.GaslessFee,
            ),
            GaslessModel(
                destination = singleUser(FEE_RECIPIENT.uppercase()),
                expected = TxInfo.TransactionType.GaslessFee,
            ),
            GaslessModel(
                destination = TransactionHistoryItem.DestinationType.Multiple(
                    addressTypes = listOf(
                        TransactionHistoryItem.AddressType.User(RECIPIENT),
                        TransactionHistoryItem.AddressType.User(FEE_RECIPIENT),
                    ),
                ),
                expected = TxInfo.TransactionType.UnknownOperation,
            ),
            // The asset is keyed with the 0x prefix; a selector parsed out of the raw call data arrives without one
            GaslessModel(
                destination = singleUser(FEE_RECIPIENT),
                methodId = GASLESS_METHOD_ID.removePrefix("0x"),
                expected = TxInfo.TransactionType.GaslessFee,
            ),
            GaslessModel(
                destination = singleUser(FEE_RECIPIENT),
                methodId = GASLESS_METHOD_ID.uppercase(),
                expected = TxInfo.TransactionType.GaslessFee,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Approve {

        private val usdt = currencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            id = "tether",
            contractAddress = USDT_CONTRACT,
            symbol = "USDT",
            decimals = 6,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ApproveModel) {
            // Arrange
            val converter = createConverter(currency = model.currency, networkTokens = model.networkTokens)

            // Act
            val actual = converter.convert(
                item(
                    destination = singleContract(model.tokenContract),
                    methodId = APPROVE_METHOD_ID,
                    callData = approveCallData(rawAllowance = model.rawAllowanceHex),
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<ApproveModel> {
            val coin = currencyFactory.createCoin(Blockchain.Ethereum)
            val viewedToken = currencyFactory.createToken(Blockchain.Ethereum, contractAddress = USDT_CONTRACT)
            return listOf(
                // Token history: the allowance is scaled by the wallet's token found by the tx destination contract
                ApproveModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    tokenContract = USDT_CONTRACT,
                    rawAllowanceHex = FIVE_USDT_RAW_HEX,
                    expected = TxInfo.TransactionType.Approve(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = BigDecimal("5.000000"),
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = USDT_CONTRACT, id = "tether"),
                        ),
                        address = SPENDER,
                    ),
                ),
                // Contract address lookup ignores case
                ApproveModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    tokenContract = USDT_CONTRACT.uppercase().replace("0X", "0x"),
                    rawAllowanceHex = FIVE_USDT_RAW_HEX,
                    expected = TxInfo.TransactionType.Approve(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = BigDecimal("5.000000"),
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = USDT_CONTRACT, id = "tether"),
                        ),
                        address = SPENDER,
                    ),
                ),
                // Untracked contract: falls back to the viewed token
                ApproveModel(
                    currency = viewedToken,
                    networkTokens = emptySet(),
                    tokenContract = USDT_CONTRACT,
                    rawAllowanceHex = FIVE_USDT_RAW_HEX,
                    expected = TxInfo.TransactionType.Approve(
                        amount = SdkAmount(
                            currencySymbol = viewedToken.symbol,
                            value = BigDecimal("0.05000000"),
                            decimals = viewedToken.decimals,
                            type = SdkAmountType.Token(
                                contractAddress = USDT_CONTRACT,
                                id = viewedToken.id.rawCurrencyId?.value,
                            ),
                        ),
                        address = SPENDER,
                    ),
                ),
                // Untracked contract in coin history: falls back to the coin
                ApproveModel(
                    currency = coin,
                    networkTokens = emptySet(),
                    tokenContract = USDT_CONTRACT,
                    rawAllowanceHex = FIVE_USDT_RAW_HEX,
                    expected = TxInfo.TransactionType.Approve(
                        amount = SdkAmount(
                            currencySymbol = coin.symbol,
                            value = BigDecimal("0.000000000005000000"),
                            decimals = coin.decimals,
                            type = SdkAmountType.Coin,
                        ),
                        address = SPENDER,
                    ),
                ),
                // Unlimited allowance (uint256 max) keeps the token identity but has no value
                ApproveModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    tokenContract = USDT_CONTRACT,
                    rawAllowanceHex = "f".repeat(64),
                    expected = TxInfo.TransactionType.Approve(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = null,
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = USDT_CONTRACT, id = "tether"),
                        ),
                        address = SPENDER,
                    ),
                ),
            )
        }

        private fun approveCallData(rawAllowance: String): String {
            val spenderWord = SPENDER.removePrefix("0x").padStart(length = 64, padChar = '0')
            val allowanceWord = rawAllowance.padStart(length = 64, padChar = '0')
            return APPROVE_METHOD_ID + spenderWord + allowanceWord
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TokenTransfer {

        private val usdt = currencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            id = "tether",
            contractAddress = USDT_CONTRACT,
            symbol = "USDT",
            decimals = 6,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: TokenTransferModel) {
            // Arrange
            val converter = createConverter(currency = model.currency, networkTokens = model.networkTokens)

            // Act
            val actual = converter.convert(
                item(
                    destination = singleContract(USDT_CONTRACT),
                    methodId = model.methodId,
                    callData = model.callData,
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TokenTransferModel> {
            val coin = currencyFactory.createCoin(Blockchain.Ethereum)
            return listOf(
                // Coin history: the call moves no coin, so it becomes the network-fee row for the tokens it sent
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    callData = transferCallData(rawAmount = FIVE_USDT_RAW_HEX),
                    expected = TxInfo.TransactionType.NetworkFee(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = BigDecimal("5.000000"),
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = USDT_CONTRACT, id = "tether"),
                        ),
                        address = RECIPIENT,
                    ),
                ),
                // Untracked contract: the sent tokens cannot be denominated, the fee row still stands
                TokenTransferModel(
                    currency = coin,
                    networkTokens = emptySet(),
                    callData = transferCallData(rawAmount = FIVE_USDT_RAW_HEX),
                    expected = TxInfo.TransactionType.NetworkFee(amount = null, address = RECIPIENT),
                ),
                // Token history: the very same call is the transfer itself
                TokenTransferModel(
                    currency = usdt,
                    networkTokens = setOf(usdt),
                    callData = transferCallData(rawAmount = FIVE_USDT_RAW_HEX),
                    expected = TxInfo.TransactionType.Transfer,
                ),
                // Call data that is not a flat ERC-20 transfer decodes to nothing and stays a plain transfer
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    callData = TRANSFER_METHOD_ID,
                    expected = TxInfo.TransactionType.Transfer,
                ),
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    callData = null,
                    expected = TxInfo.TransactionType.Transfer,
                ),
                // A selector parsed out of the raw call data carries no 0x prefix, and still resolves to `transfer`
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(usdt),
                    methodId = TRANSFER_METHOD_ID.removePrefix("0x"),
                    callData = transferCallData(rawAmount = FIVE_USDT_RAW_HEX),
                    expected = TxInfo.TransactionType.NetworkFee(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = BigDecimal("5.000000"),
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = USDT_CONTRACT, id = "tether"),
                        ),
                        address = RECIPIENT,
                    ),
                ),
            )
        }

        private fun transferCallData(rawAmount: String): String {
            val recipientWord = RECIPIENT.removePrefix("0x").padStart(length = 64, padChar = '0')
            val amountWord = rawAmount.padStart(length = 64, padChar = '0')
            return TRANSFER_METHOD_ID + recipientWord + amountWord
        }
    }

    private fun createConverter(
        currency: CryptoCurrency,
        networkTokens: Set<CryptoCurrency.Token> = emptySet(),
    ) = SdkTransactionTypeConverter(
        smartContractMethods = mapOf(
            GASLESS_METHOD_ID to SmartContractMethod(info = null, source = null, name = "gaslessTransaction"),
            APPROVE_METHOD_ID to SmartContractMethod(info = null, source = null, name = "approve"),
            TRANSFER_METHOD_ID to SmartContractMethod(info = null, source = null, name = "transfer"),
        ),
        yieldSupplyAddresses = emptySet(),
        gaslessFeeAddresses = setOf(FEE_RECIPIENT),
        currency = currency,
        networkTokens = networkTokens,
    )

    private fun item(
        destination: TransactionHistoryItem.DestinationType,
        methodId: String,
        callData: String? = null,
    ) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = true,
        destinationType = destination,
        sourceType = TransactionHistoryItem.SourceType.Single(SENDER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.ContractMethod(id = methodId, callData = callData),
        amount = mockk(),
        fee = mockk(),
    )

    private fun singleUser(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.User(address),
    )

    private fun singleContract(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.Contract(address),
    )

    internal data class GaslessModel(
        val destination: TransactionHistoryItem.DestinationType,
        val expected: TxInfo.TransactionType,
        val methodId: String = GASLESS_METHOD_ID,
    )

    internal data class TokenTransferModel(
        val currency: CryptoCurrency,
        val networkTokens: Set<CryptoCurrency.Token>,
        val callData: String?,
        val expected: TxInfo.TransactionType,
        val methodId: String = TRANSFER_METHOD_ID,
    )

    internal data class ApproveModel(
        val currency: CryptoCurrency,
        val networkTokens: Set<CryptoCurrency.Token>,
        val tokenContract: String,
        val rawAllowanceHex: String,
        val expected: TxInfo.TransactionType,
    )

    private companion object {
        const val GASLESS_METHOD_ID = "0x6234d42b"
        const val APPROVE_METHOD_ID = "0x095ea7b3"
        const val TRANSFER_METHOD_ID = "0xa9059cbb"
        const val SENDER = "0x9ffd974772bda94d288240c1b22f367ce75ccd7f"
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val FEE_RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SPENDER = "0x3333333333333333333333333333333333333333"
        const val USDT_CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7"

        /** 5 USDT in the token's smallest units (6 decimals): 5_000_000 = 0x4c4b40. */
        const val FIVE_USDT_RAW_HEX = "4c4b40"
    }
}