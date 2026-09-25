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
import com.tangem.domain.models.network.Network
import java.math.BigInteger
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            // The method is recognized, only its destination is not — the row falls back to the method's own name
            GaslessModel(
                destination = singleUser(RECIPIENT),
                expected = TxInfo.TransactionType.Operation(name = "GaslessTransaction"),
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
                // A send that moved no coin only cost the fee — left unrecognized, so the history shows a fee row
                YieldSendModel(
                    currency = coin,
                    amount = BigDecimal.ZERO,
                    expected = TxInfo.TransactionType.Operation(name = "YieldSend"),
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
    inner class Joint {

        private val jointNetwork: Network = currencyFactory.ethereum.network.let { network ->
            val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")
            network.copy(
                id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
                derivationPath = derivationPath,
            )
        }
        private val coin: CryptoCurrency.Coin = currencyFactory.ethereum.copy(network = jointNetwork)
        private val token: CryptoCurrency.Token = currencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            contractAddress = USDT_CONTRACT,
            symbol = "USDT",
            decimals = 6,
        ).copy(network = jointNetwork)

        @Test
        fun `GIVEN native transfer calldata in coin history WHEN convert THEN joint transfer of the coin`() {
            // Act
            val actual = jointConverter(coin).convert(safeItem(SAFE_EXEC_METHOD_ID, EXEC_TRANSACTION_CALLDATA))

            // Assert
            assertThat(actual).isEqualTo(
                TxInfo.TransactionType.Joint.Transfer(
                    recipient = SAFE_RECIPIENT,
                    amount = BigDecimal("10.935223680498695559"),
                    refundCapWei = REFUND_CAP_WEI,
                    signaturesCount = 2,
                ),
            )
        }

        @Test
        fun `GIVEN aggregate3 bundle WHEN convert THEN the bundled transfer is read`() {
            // Act
            val actual = jointConverter(coin).convert(safeItem(AGGREGATE3_METHOD_ID, AGGREGATE3_CALLDATA))

            // Assert
            assertThat(actual).isEqualTo(
                TxInfo.TransactionType.Joint.Transfer(
                    recipient = SAFE_RECIPIENT,
                    amount = BigDecimal("1.093522368049869555"),
                    refundCapWei = BigDecimal("241162565430269000"),
                    signaturesCount = 2,
                ),
            )
        }

        @Test
        fun `GIVEN token transfer calldata in coin history WHEN convert THEN token fee with the token amount`() {
            // Act
            val actual = jointConverter(coin).convert(safeItem(SAFE_EXEC_METHOD_ID, tokenTransferCalldata()))

            // Assert
            assertThat(actual).isEqualTo(
                TxInfo.TransactionType.Joint.TokenFee(
                    tokenContract = USDT_CONTRACT,
                    tokenAmount = SdkAmount(
                        currencySymbol = "USDT",
                        value = BigDecimal("1.000000"),
                        decimals = 6,
                        type = SdkAmountType.Token(contractAddress = USDT_CONTRACT, id = token.id.rawCurrencyId?.value),
                    ),
                    recipient = RECIPIENT,
                    refundCapWei = REFUND_CAP_WEI,
                    signaturesCount = 2,
                ),
            )
        }

        @Test
        fun `GIVEN token transfer calldata in token history WHEN convert THEN joint transfer of the token`() {
            // Act
            val actual = jointConverter(token).convert(safeItem(SAFE_EXEC_METHOD_ID, tokenTransferCalldata()))

            // Assert
            assertThat(actual).isEqualTo(
                TxInfo.TransactionType.Joint.Transfer(
                    recipient = RECIPIENT,
                    amount = BigDecimal("1.000000"),
                    refundCapWei = REFUND_CAP_WEI,
                    signaturesCount = 2,
                ),
            )
        }

        @Test
        fun `GIVEN unknown inner call WHEN convert THEN joint call against the contract`() {
            // Arrange — the inner data is an approve(address,uint256), not a transfer
            val item = safeItem(SAFE_EXEC_METHOD_ID, innerCallCalldata(innerTo = USDT_CONTRACT, innerData = APPROVE_DATA))

            // Act
            val actual = jointConverter(coin).convert(item)

            // Assert
            assertThat(actual).isEqualTo(
                TxInfo.TransactionType.Joint.Call(
                    contract = USDT_CONTRACT,
                    value = BigInteger.ZERO.toBigDecimal().movePointLeft(18),
                    refundCapWei = REFUND_CAP_WEI,
                    signaturesCount = 2,
                ),
            )
        }

        @Test
        fun `GIVEN personal account calling a Safe WHEN convert THEN plain named operation`() {
            // Act
            val actual = jointConverter(currencyFactory.ethereum)
                .convert(safeItem(SAFE_EXEC_METHOD_ID, EXEC_TRANSACTION_CALLDATA))

            // Assert
            assertThat(actual).isEqualTo(TxInfo.TransactionType.Operation(name = "ExecTransaction"))
        }

        @Test
        fun `GIVEN execTransaction with a broken payload WHEN convert THEN plain named operation`() {
            // Act
            val actual = jointConverter(coin).convert(safeItem(SAFE_EXEC_METHOD_ID, EXEC_TRANSACTION_CALLDATA.take(200)))

            // Assert
            assertThat(actual).isEqualTo(TxInfo.TransactionType.Operation(name = "ExecTransaction"))
        }

        private fun jointConverter(currency: CryptoCurrency) = SdkTransactionTypeConverter(
            smartContractMethods = mapOf(
                SAFE_EXEC_METHOD_ID to SmartContractMethod(info = "Safe", source = null, name = "execTransaction"),
                AGGREGATE3_METHOD_ID to SmartContractMethod(info = "Multicall3", source = null, name = "aggregate3"),
            ),
            yieldSupplyAddresses = emptySet(),
            gaslessFeeAddresses = emptySet(),
            currency = currency,
            networkTokens = setOf(token),
            getCachedTokenInfoUseCase = mockk(relaxed = true),
        )

        /** The provider record: relayer → Safe, zero value, the calldata on the type, the fee in the network coin */
        private fun safeItem(methodId: String, callData: String) = TransactionHistoryItem(
            txHash = "0xhash",
            timestamp = 0L,
            isOutgoing = false,
            destinationType = singleUser(SAFE_ADDRESS),
            sourceType = TransactionHistoryItem.SourceType.Single(RELAYER),
            status = TransactionHistoryItem.TransactionStatus.Confirmed,
            type = TransactionHistoryItem.TransactionType.ContractMethod(id = methodId, callData = callData),
            amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
            fee = Amount(value = BigDecimal("0.0001"), blockchain = Blockchain.Ethereum),
        )

        /** `execTransaction` around `transfer(RECIPIENT, 1 USDT)` on [USDT_CONTRACT] */
        private fun tokenTransferCalldata(): String = innerCallCalldata(
            innerTo = USDT_CONTRACT,
            innerData = TRANSFER_SELECTOR + RECIPIENT.removePrefix("0x").padStart(64, '0') +
                ONE_USDT_RAW.toString(16).padStart(64, '0'),
        )

        /**
         * Re-encodes the direct fixture around another inner call: the head words are rewritten, the dynamic `data`
         * sits at offset 320 and the signatures follow it word-aligned, exactly as the ABI lays them out.
         */
        private fun innerCallCalldata(innerTo: String, innerData: String): String {
            fun word(value: Long) = value.toString(16).padStart(64, '0')
            fun addressWord(address: String) = address.removePrefix("0x").lowercase().padStart(64, '0')
            val dataBytes = innerData.length / 2
            val paddedData = innerData.padEnd((dataBytes + 31) / 32 * 64, '0')
            val signaturesOffset = 320 + 32 + paddedData.length / 2
            val signatures = "11".repeat(65) + "22".repeat(65)
            return "0x6a761202" +
                addressWord(innerTo) + word(0) + word(320) + word(0) +
                word(31_500) + word(110_000) + word(446_585_022_236) + addressWord(ZERO_ADDRESS) + addressWord(RELAYER) +
                word(signaturesOffset.toLong()) +
                word(dataBytes.toLong()) + paddedData +
                word(130) + signatures
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

    private fun singleUser(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.User(address),
    )

    private fun singleContract(address: String) = TransactionHistoryItem.DestinationType.Single(
        addressType = TransactionHistoryItem.AddressType.Contract(address),
    )

    internal data class YieldSendModel(
        val currency: CryptoCurrency,
        val amount: BigDecimal,
        val expected: TxInfo.TransactionType,
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
        const val SAFE_EXEC_METHOD_ID = "0x6a761202"
        const val AGGREGATE3_METHOD_ID = "0x82ad56cb"
        const val TRANSFER_SELECTOR = "a9059cbb"
        const val SAFE_ADDRESS = "0x4cd2b9a4197dd56e8bde666691a18b2bfa9d3c74"
        const val RELAYER = "0x5f2c435df8d00d0cf842a52dd166b1dcd69734c2"
        const val SAFE_RECIPIENT = "0x6dbd0c1957dfe9d85f74b8fe3a355b268a39ec91"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        const val ONE_USDT_RAW = 1_000_000L
        const val APPROVE_DATA = "095ea7b3" +
            "0000000000000000000000006dbd0c1957dfe9d85f74b8fe3a355b268a39ec91" +
            "0000000000000000000000000000000000000000000000000de0b6b3a7640000"

        /** `(31500 + 110000) × 446585022236`: the refund cap of the direct Polygon fixture */
        val REFUND_CAP_WEI = BigDecimal("63191780646394000")

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

        const val AGGREGATE3_CALLDATA = "0x" +
            "82ad56cb0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000" +
            "000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000" +
            "000000400000000000000000000000000000000000000000000000000000000000000300000000000000000000000000" +
            "14f2982d601c9458f93bd70b218933a6f8165e7b00000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000060000000000000000000000000" +
            "00000000000000000000000000000000000002241688f0b9000000000000000000000000ff51a5898e281db6dfc78557" +
            "90607438df2ca44b00000000000000000000000000000000000000000000000000000000000000600000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000184b63e800d000000000000000000000000000000000000000000000000000000000000010000000000" +
            "000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000016000000000" +
            "00000000000000003efcbb83a4a7afcb4f68d501e2c2203a38be77f40000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000200000000000000000000000019bea1fdf4e79fbf9282daca0ba301d17e2521bb00000000" +
            "000000000000000083671c8f2b98affbeba363b4fbdac111b04188100000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000004cd2b9a4197dd56e8bde666691a18b2b" +
            "fa9d3c740000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000" +
            "000002246a7612020000000000000000000000006dbd0c1957dfe9d85f74b8fe3a355b268a39ec910000000000000000" +
            "000000000000000000000000000000000f2cf8cccd8e72f3000000000000000000000000000000000000000000000000" +
            "000000000000014000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000007b0c000000000000000000000000000000000000000000000000" +
            "00000000000641900000000000000000000000000000000000000000000000000000007f2e1e2d9e0000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000000000005f2c435df8d00d0cf842a52d" +
            "d166b1dcd69734c200000000000000000000000000000000000000000000000000000000000001600000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000826b17506d2f5fd387855528ed4c211588f80f02b9d699d4e2868fe7ac7f72b3c432c5105ae3432a4d" +
            "1f96697c195c51cdf4144c7c81a64b0edd6af5c998e7e5eb1c8a929273755e691a54c169cd7447d15e9d1e4c8d2ce9af" +
            "5f02062c1f15ca2b3547494ffa197d704e967c2cd92be66632a92e70065d8ef08a6e27c5e2b035ab4b1b000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000"
        const val SENDER = "0x9ffd974772bda94d288240c1b22f367ce75ccd7f"
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val FEE_RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SPENDER = "0x3333333333333333333333333333333333333333"
        const val USDT_CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7"

        /** 5 USDT in the token's smallest units (6 decimals): 5_000_000 = 0x4c4b40. */
        const val FIVE_USDT_RAW_HEX = "4c4b40"
    }
}