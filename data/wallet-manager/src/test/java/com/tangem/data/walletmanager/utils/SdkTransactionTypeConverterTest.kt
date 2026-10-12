package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
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

        private val usdt = currencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            id = "tether",
            contractAddress = USDT_CONTRACT,
            symbol = "USDT",
            decimals = 6,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: GaslessModel) {
            // Arrange
            val converter = createConverter(currency = model.currency)

            // Act
            val actual = converter.convert(item(destination = model.destination, methodId = model.methodId))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // The call moved no coin, so the coin history keeps it an operation and the fee-only rules take it from
            // there; in the token's own history the very same leg is the transfer the user made
            GaslessModel(
                destination = singleUser(RECIPIENT),
                expected = TxInfo.TransactionType.Operation(name = "GaslessTransaction"),
            ),
            GaslessModel(
                currency = usdt,
                destination = singleUser(RECIPIENT),
                expected = TxInfo.TransactionType.Transfer,
            ),
            GaslessModel(
                currency = usdt,
                destination = singleUser(FEE_RECIPIENT),
                expected = TxInfo.TransactionType.GaslessFee,
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
                expected = TxInfo.TransactionType.Operation(name = "GaslessTransaction"),
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TronTokenTransfer {

        private val tronUsdt = currencyFactory.createToken(
            blockchain = Blockchain.Tron,
            id = "tether",
            contractAddress = TRON_USDT_CONTRACT,
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
                    destination = singleContract(TRON_USDT_CONTRACT),
                    methodId = model.methodId,
                    callData = model.callData,
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TokenTransferModel> {
            val coin = currencyFactory.createCoin(Blockchain.Tron)
            return listOf(
                // The TRC-20 recipient is decoded back into the base58 form the rest of the app speaks
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(tronUsdt),
                    callData = TRON_TRANSFER_CALL_DATA,
                    expected = TxInfo.TransactionType.NetworkFee(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = BigDecimal("5.000000"),
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = TRON_USDT_CONTRACT, id = "tether"),
                        ),
                        address = TRON_RECIPIENT,
                    ),
                ),
                TokenTransferModel(
                    currency = coin,
                    networkTokens = emptySet(),
                    callData = TRON_TRANSFER_CALL_DATA,
                    expected = TxInfo.TransactionType.NetworkFee(amount = null, address = TRON_RECIPIENT),
                ),
                // Token history: the very same call is the transfer itself
                TokenTransferModel(
                    currency = tronUsdt,
                    networkTokens = setOf(tronUsdt),
                    callData = TRON_TRANSFER_CALL_DATA,
                    expected = TxInfo.TransactionType.Transfer,
                ),
                // A wallet reusing the EVM encoding leaves the prefix byte zero and means the same recipient
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(tronUsdt),
                    callData = TRANSFER_METHOD_ID + TRON_SPENDER_BODY.padStart(64, '0') +
                        FIVE_USDT_RAW_HEX.padStart(64, '0'),
                    expected = TxInfo.TransactionType.NetworkFee(
                        amount = SdkAmount(
                            currencySymbol = "USDT",
                            value = BigDecimal("5.000000"),
                            decimals = 6,
                            type = SdkAmountType.Token(contractAddress = TRON_USDT_CONTRACT, id = "tether"),
                        ),
                        address = TRON_SPENDER,
                    ),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TronApprove {

        private val tronUsdt = currencyFactory.createToken(
            blockchain = Blockchain.Tron,
            id = "tether",
            contractAddress = TRON_USDT_CONTRACT,
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
                    destination = singleContract(TRON_USDT_CONTRACT),
                    methodId = model.methodId,
                    callData = model.callData,
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TokenTransferModel> {
            val coin = currencyFactory.createCoin(Blockchain.Tron)
            val approved = SdkAmount(
                currencySymbol = "USDT",
                value = BigDecimal("5.000000"),
                decimals = 6,
                type = SdkAmountType.Token(contractAddress = TRON_USDT_CONTRACT, id = "tether"),
            )
            return listOf(
                // The spender is named in base58, the only form a Tron address has
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(tronUsdt),
                    methodId = APPROVE_METHOD_ID,
                    callData = tronApproveCallData(spender = "41" + TRON_SPENDER_BODY),
                    expected = TxInfo.TransactionType.Approve(amount = approved, address = TRON_SPENDER),
                ),
                // A wallet reusing the EVM encoding leaves the prefix byte zero and means the same spender
                TokenTransferModel(
                    currency = coin,
                    networkTokens = setOf(tronUsdt),
                    methodId = APPROVE_METHOD_ID,
                    callData = tronApproveCallData(spender = TRON_SPENDER_BODY),
                    expected = TxInfo.TransactionType.Approve(amount = approved, address = TRON_SPENDER),
                ),
            )
        }

        private fun tronApproveCallData(spender: String): String =
            APPROVE_METHOD_ID +
                spender.padStart(length = 64, padChar = '0') +
                FIVE_USDT_RAW_HEX.padStart(length = 64, padChar = '0')
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PlainTransfer {

        private val tronUsdt = currencyFactory.createToken(
            blockchain = Blockchain.Tron,
            id = "tether",
            contractAddress = TRON_USDT_CONTRACT,
            symbol = "USDT",
            decimals = 6,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: PlainTransferModel) {
            // Arrange
            val converter = createConverter(
                currency = model.currency,
                gaslessFeeAddresses = setOf(TRON_FEE_RECIPIENT),
            )

            // Act
            val actual = converter.convert(
                transferItem(destination = model.destination, isOutgoing = model.isOutgoing),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // The leg that paid for the gasless transfer, told apart by its recipient alone
            PlainTransferModel(
                currency = tronUsdt,
                destination = singleUser(TRON_FEE_RECIPIENT),
                expected = TxInfo.TransactionType.GaslessFee,
            ),
            // The leg that reached the recipient the user typed
            PlainTransferModel(
                currency = tronUsdt,
                destination = singleUser(TRON_RECIPIENT),
                expected = TxInfo.TransactionType.Transfer,
            ),
            // Tokens arriving from the fee collector are not a fee the user paid
            PlainTransferModel(
                currency = tronUsdt,
                destination = singleUser(TRON_FEE_RECIPIENT),
                isOutgoing = false,
                expected = TxInfo.TransactionType.Transfer,
            ),
            PlainTransferModel(
                currency = tronUsdt,
                destination = TransactionHistoryItem.DestinationType.Multiple(
                    addressTypes = listOf(
                        TransactionHistoryItem.AddressType.User(TRON_RECIPIENT),
                        TransactionHistoryItem.AddressType.User(TRON_FEE_RECIPIENT),
                    ),
                ),
                expected = TxInfo.TransactionType.Transfer,
            ),
            // The fee is paid in the token, so a TRX transfer to the same address is an ordinary send
            PlainTransferModel(
                currency = currencyFactory.createCoin(Blockchain.Tron),
                destination = singleUser(TRON_FEE_RECIPIENT),
                expected = TxInfo.TransactionType.Transfer,
            ),
            // Off Tron the address alone decides nothing - the EVM fee leg is identified by its method
            PlainTransferModel(
                currency = currencyFactory.createToken(Blockchain.Ethereum, contractAddress = USDT_CONTRACT),
                destination = singleUser(TRON_FEE_RECIPIENT),
                expected = TxInfo.TransactionType.Transfer,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldSend {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: YieldSendModel) {
            // Arrange
            val converter = createConverter(currency = model.currency)

            // Act
            val actual = converter.convert(yieldSendItem(amount = model.amount))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<YieldSendModel> {
            val coin = currencyFactory.createCoin(Blockchain.Ethereum)
            val token = currencyFactory.createToken(Blockchain.Ethereum, contractAddress = USDT_CONTRACT)
            return listOf(
                // A send that moved no coin only cost the fee, so the history shows a fee row. The send rides along,
                // but it changed no yield mode — the row has nothing to name but the method itself.
                YieldSendModel(
                    currency = coin,
                    amount = BigDecimal.ZERO,
                    expected = TxInfo.TransactionType.Operation(
                        name = "YieldSend",
                        recognized = TxInfo.TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = false),
                    ),
                ),
                YieldSendModel(
                    currency = coin,
                    amount = BigDecimal.ONE,
                    expected = TxInfo.TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = false),
                ),
                // The token history is the transfer itself, whatever the coin side of it moved
                YieldSendModel(
                    currency = token,
                    amount = BigDecimal.ZERO,
                    expected = TxInfo.TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = false),
                ),
            )
        }

        private fun yieldSendItem(amount: BigDecimal) = TransactionHistoryItem(
            txHash = "0xhash",
            timestamp = 0L,
            isOutgoing = true,
            destinationType = singleContract(RECIPIENT),
            sourceType = TransactionHistoryItem.SourceType.Single(SENDER),
            status = TransactionHistoryItem.TransactionStatus.Confirmed,
            type = TransactionHistoryItem.TransactionType.ContractMethodName(name = "yieldSend"),
            amount = Amount(currencySymbol = "ETH", value = amount, decimals = 18),
            fee = mockk(),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldModuleCall {

        private val converter = createConverter(currency = currencyFactory.createCoin(Blockchain.Ethereum))

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: YieldModuleCallModel) {
            // Arrange
            val item = item(
                destination = singleContract(YIELD_MODULE_CONTRACT),
                methodId = model.methodId,
                callData = yieldCallData(model.methodId),
                amount = BigDecimal.ZERO,
            )

            // Act
            val actual = converter.convert(item)

            // Assert
            assertThat(actual).isEqualTo(
                TxInfo.TransactionType.Operation(name = model.name, recognized = model.recognized),
            )
        }

        /**
         * The coin only paid the gas of a call the token made, so the row is a plain operation — still carrying the
         * call it was decoded into, which is what the network-fee row names.
         */
        private fun provideTestModels() = listOf(
            YieldModuleCallModel(
                methodId = ENTER_METHOD_ID,
                name = "EnterProtocolByOwner",
                recognized = TxInfo.TransactionType.YieldSupply.Enter(address = USDT_CONTRACT),
            ),
            YieldModuleCallModel(
                methodId = EXIT_METHOD_ID,
                name = "WithdrawAndDeactivate",
                recognized = TxInfo.TransactionType.YieldSupply.Exit(address = USDT_CONTRACT),
            ),
            YieldModuleCallModel(
                methodId = TOPUP_METHOD_ID,
                name = "SupplyTopUp",
                recognized = TxInfo.TransactionType.YieldSupply.Topup,
            ),
            YieldModuleCallModel(
                methodId = DEPLOY_METHOD_ID,
                name = "DeployYieldModule",
                recognized = TxInfo.TransactionType.YieldSupply.DeployContract(address = YIELD_MODULE_CONTRACT),
            ),
            YieldModuleCallModel(
                methodId = INIT_METHOD_ID,
                name = "InitYieldToken",
                recognized = TxInfo.TransactionType.YieldSupply.InitializeToken(address = USDT_CONTRACT),
            ),
            YieldModuleCallModel(
                methodId = REACTIVATE_METHOD_ID,
                name = "ReactivateToken",
                recognized = TxInfo.TransactionType.YieldSupply.ReactivateToken(address = USDT_CONTRACT),
            ),
        )

        /** Every yield-module call carries the token it acts on in its first ABI word. */
        private fun yieldCallData(methodId: String): String =
            methodId + USDT_CONTRACT.removePrefix("0x").padStart(WORD_HEX_LENGTH, '0')
    }

    private fun createConverter(
        currency: CryptoCurrency,
        networkTokens: Set<CryptoCurrency.Token> = emptySet(),
        gaslessFeeAddresses: Set<String> = setOf(FEE_RECIPIENT),
    ) = SdkTransactionTypeConverter(
        smartContractMethods = mapOf(
            GASLESS_METHOD_ID to SmartContractMethod(info = null, source = null, name = "gaslessTransaction"),
            APPROVE_METHOD_ID to SmartContractMethod(info = null, source = null, name = "approve"),
            TRANSFER_METHOD_ID to SmartContractMethod(info = null, source = null, name = "transfer"),
            ENTER_METHOD_ID to SmartContractMethod(info = null, source = null, name = "enterProtocolByOwner"),
            EXIT_METHOD_ID to SmartContractMethod(info = null, source = null, name = "withdrawAndDeactivate"),
            TOPUP_METHOD_ID to SmartContractMethod(info = null, source = null, name = "supplyTopUp"),
            DEPLOY_METHOD_ID to SmartContractMethod(info = null, source = null, name = "deployYieldModule"),
            INIT_METHOD_ID to SmartContractMethod(info = null, source = null, name = "initYieldToken"),
            REACTIVATE_METHOD_ID to SmartContractMethod(info = null, source = null, name = "reactivateToken"),
        ),
        yieldSupplyAddresses = emptySet(),
        gaslessFeeAddresses = gaslessFeeAddresses,
        currency = currency,
        networkTokens = networkTokens,
        getCachedTokenInfoUseCase = mockk(relaxed = true),
    )

    private fun item(
        destination: TransactionHistoryItem.DestinationType,
        methodId: String,
        callData: String? = null,
        amount: BigDecimal = BigDecimal.ONE,
    ) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = true,
        destinationType = destination,
        sourceType = TransactionHistoryItem.SourceType.Single(SENDER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.ContractMethod(id = methodId, callData = callData),
        amount = Amount(currencySymbol = "ETH", value = amount, decimals = 18),
        fee = mockk(),
    )

    private fun transferItem(
        destination: TransactionHistoryItem.DestinationType,
        isOutgoing: Boolean,
    ) = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = isOutgoing,
        destinationType = destination,
        sourceType = TransactionHistoryItem.SourceType.Single(SENDER),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.Transfer,
        amount = Amount(currencySymbol = "USDT", value = BigDecimal.ONE, decimals = 6),
        fee = mockk(),
    )

    private fun singleUser(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.User(address),
    )

    private fun singleContract(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.Contract(address),
    )

    internal data class YieldModuleCallModel(
        val methodId: String,
        val name: String,
        val recognized: TxInfo.TransactionType.YieldSupply,
    )

    internal data class YieldSendModel(
        val currency: CryptoCurrency,
        val amount: BigDecimal,
        val expected: TxInfo.TransactionType,
    )

    internal data class PlainTransferModel(
        val currency: CryptoCurrency,
        val destination: TransactionHistoryItem.DestinationType,
        val expected: TxInfo.TransactionType,
        val isOutgoing: Boolean = true,
    )

    internal data class GaslessModel(
        val destination: TransactionHistoryItem.DestinationType,
        val expected: TxInfo.TransactionType,
        val methodId: String = GASLESS_METHOD_ID,
        val currency: CryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum),
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
        const val TRON_USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val TRON_RECIPIENT = "TSrqiA137hwRbUQwARCCzYCEZVMfFyHap3"
        const val TRON_FEE_RECIPIENT = "TSWsmaEDgWaE2VsP9tCBGh5GbBRbrdHSuZ"
        const val TRON_SPENDER = "TG5wUqBkukAho2E38ca3EZG4zvYp3hUivZ"
        const val TRON_SPENDER_BODY = "4316b5c3f99fd4818918abced9d7df3d31396ae3"
        const val TRON_TRANSFER_CALL_DATA = "0xa9059cbb000000000000000000000041b945de8e80a32a1bf5aa55038d" +
            "69a1c90f1eca3f00000000000000000000000000000000000000000000000000000000004c4b40"
        const val SENDER = "0x9ffd974772bda94d288240c1b22f367ce75ccd7f"
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val FEE_RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SPENDER = "0x3333333333333333333333333333333333333333"
        const val USDT_CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7"

        const val ENTER_METHOD_ID = "0x79be55f7"
        const val EXIT_METHOD_ID = "0xc65e6dcf"
        const val TOPUP_METHOD_ID = "0xb9de6a93"
        const val DEPLOY_METHOD_ID = "0xcbeda14c"
        const val INIT_METHOD_ID = "0xebd4b81c"
        const val REACTIVATE_METHOD_ID = "0xc478e956"
        const val YIELD_MODULE_CONTRACT = "0x4444444444444444444444444444444444444444"

        /** An ABI word, in hex characters: 32 bytes. */
        const val WORD_HEX_LENGTH = 64

        /** 5 USDT in the token's smallest units (6 decimals): 5_000_000 = 0x4c4b40. */
        const val FIVE_USDT_RAW_HEX = "4c4b40"
    }
}