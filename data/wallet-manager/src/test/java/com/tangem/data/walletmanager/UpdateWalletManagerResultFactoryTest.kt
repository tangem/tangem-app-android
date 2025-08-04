package com.tangem.data.walletmanager

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.walletmanager.MockUpdateWalletManagerResultFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Calendar

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateWalletManagerResultFactoryTest {

    private val factory = UpdateWalletManagerResultFactory()
    private val mockFactory = MockUpdateWalletManagerResultFactory()

    private val walletManager = mockk<WalletManager>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManager)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetResult {

        @ParameterizedTest
        @ProvideTestModels
        fun getResult(model: GetResultTestModel) {
            // Arrange
            every { walletManager.wallet } returns model.wallet

            // Act
            val actual = factory.getResult(walletManager = walletManager)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<GetResultTestModel> = listOf(
            // region Wallet without amount and transactions
            GetResultTestModel(
                wallet = createWallet(
                    coinValue = null,
                    addresses = setOf(Address(value = "0x1", type = AddressType.Default)),
                    transactions = emptyList(),
                ),
                expected = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                    currenciesAmounts = emptySet(),
                    currentTransactions = emptySet(),
                ),
            ),
            // endregion

            // region Wallet with coin amount and without transactions
            GetResultTestModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                    ),
                    transactions = emptyList(),
                ),
                expected = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                    currenciesAmounts = setOf(
                        UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ONE),
                    ),
                    currentTransactions = emptySet(),
                ),
            ),
            // endregion

            // region Wallet with coin amount and coin transaction
            GetResultTestModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                    ),
                    transactions = listOf(
                        createRecentTransaction(
                            amount = Amount(
                                blockchain = Blockchain.Ethereum,
                                value = BigDecimal.ONE,
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                    ),
                ),
                expected = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                    currenciesAmounts = setOf(
                        UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ONE),
                    ),
                    currentTransactions = setOf(
                        UpdateWalletManagerResult.CryptoCurrencyTransaction.Coin(
                            txInfo = createTxInfo(
                                status = TxInfo.TransactionStatus.Unconfirmed,
                                amount = BigDecimal.ONE,
                            ),
                        ),
                    ),
                ),
            ),
            // endregion

            // region Wallet with amounts and transactions (coin and token)
            GetResultTestModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    tokensAmount = mapOf(usdtToken to BigDecimal.ZERO),
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                        Address(value = "0x11", type = AddressType.Legacy),
                    ),
                    transactions = listOf(
                        createRecentTransaction(
                            amount = Amount(
                                blockchain = Blockchain.Ethereum,
                                value = BigDecimal.ONE,
                            ),
                            status = TransactionStatus.Confirmed,
                        ),
                        createRecentTransaction(
                            amount = Amount(
                                value = BigDecimal.TEN,
                                blockchain = Blockchain.Ethereum,
                                type = AmountType.Token(token = usdtToken),
                                currencySymbol = "USDT",
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                    ),
                ),
                expected = UpdateWalletManagerResult.Verified(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                        UpdateWalletManagerResult.Address(
                            value = "0x11",
                            type = UpdateWalletManagerResult.Address.Type.Secondary,
                        ),
                    ),
                    currenciesAmounts = setOf(
                        UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ONE),
                        UpdateWalletManagerResult.CryptoCurrencyAmount.Token(
                            value = BigDecimal.ZERO,
                            currencyRawId = usdtToken.id?.let(CryptoCurrency::RawID),
                            contractAddress = usdtToken.contractAddress,
                        ),
                    ),
                    currentTransactions = setOf(
                        UpdateWalletManagerResult.CryptoCurrencyTransaction.Token(
                            txInfo = createTxInfo(
                                status = TxInfo.TransactionStatus.Unconfirmed,
                                amount = BigDecimal.TEN,
                            ),
                            tokenId = "0x3",
                            contractAddress = "0x4",
                        ),
                    ),
                ),
            ),
            // endregion
        )
    }

    data class GetResultTestModel(val wallet: Wallet, val expected: UpdateWalletManagerResult.Verified)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetDemoResult {

        @ParameterizedTest
        @ProvideTestModels
        fun getDemoResult(model: GetDemoResultTestModel) {
            // Arrange
            every { walletManager.wallet } returns model.wallet
            every { walletManager.cardTokens } returns model.cardTokens.toMutableSet()

            // Act
            val actual = factory.getDemoResult(walletManager = walletManager, demoAmount = model.demoAmount)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<GetDemoResultTestModel> {
            return listOf(
                // region Wallet without amount and transactions
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = null,
                        addresses = setOf(Address(value = "0x1", type = AddressType.Default)),
                        transactions = emptyList(),
                    ),
                    demoAmount = Amount(value = null, blockchain = Blockchain.Ethereum),
                    cardTokens = emptySet(),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ZERO), // default for demo
                        ),
                        currentTransactions = emptySet(),
                    ),
                ),
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = null,
                        addresses = setOf(Address(value = "0x1", type = AddressType.Default)),
                        transactions = emptyList(),
                    ),
                    demoAmount = Amount(value = BigDecimal.ONE, blockchain = Blockchain.Ethereum),
                    cardTokens = emptySet(),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ONE), // used demo amount
                        ),
                        currentTransactions = emptySet(),
                    ),
                ),
                // endregion

                // region Wallet with coin amount and without transactions
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = BigDecimal.ONE,
                        addresses = setOf(
                            Address(value = "0x1", type = AddressType.Default),
                        ),
                        transactions = emptyList(),
                    ),
                    demoAmount = Amount(value = null, blockchain = Blockchain.Ethereum),
                    cardTokens = emptySet(),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ZERO),
                        ),
                        currentTransactions = emptySet(),
                    ),
                ),
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = BigDecimal.ONE,
                        addresses = setOf(
                            Address(value = "0x1", type = AddressType.Default),
                        ),
                        transactions = emptyList(),
                    ),
                    demoAmount = Amount(value = BigDecimal.TEN, blockchain = Blockchain.Ethereum),
                    cardTokens = emptySet(),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.TEN),
                        ),
                        currentTransactions = emptySet(),
                    ),
                ),
                // endregion

                // region Wallet with coin amount and coin transaction
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = BigDecimal.ONE,
                        addresses = setOf(
                            Address(value = "0x1", type = AddressType.Default),
                        ),
                        transactions = listOf(
                            createRecentTransaction(
                                amount = Amount(
                                    blockchain = Blockchain.Ethereum,
                                    value = BigDecimal.ONE,
                                ),
                                status = TransactionStatus.Unconfirmed,
                            ),
                        ),
                    ),
                    demoAmount = Amount(value = BigDecimal.TEN, blockchain = Blockchain.Ethereum),
                    cardTokens = emptySet(),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.TEN),
                        ),
                        currentTransactions = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyTransaction.Coin(
                                txInfo = createTxInfo(
                                    status = TxInfo.TransactionStatus.Unconfirmed,
                                    amount = BigDecimal.ONE,
                                ),
                            ),
                        ),
                    ),
                ),
                // endregion

                // region Wallet with amounts and transactions (coin and token)
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = BigDecimal.TEN,
                        tokensAmount = mapOf(usdtToken to BigDecimal.TEN),
                        addresses = setOf(
                            Address(value = "0x1", type = AddressType.Default),
                            Address(value = "0x11", type = AddressType.Legacy),
                        ),
                        transactions = listOf(
                            createRecentTransaction(
                                amount = Amount(
                                    blockchain = Blockchain.Ethereum,
                                    value = BigDecimal.ONE,
                                ),
                                status = TransactionStatus.Confirmed,
                            ),
                            createRecentTransaction(
                                amount = Amount(
                                    value = BigDecimal.TEN,
                                    blockchain = Blockchain.Ethereum,
                                    type = AmountType.Token(token = usdtToken),
                                    currencySymbol = "USDT",
                                ),
                                status = TransactionStatus.Unconfirmed,
                            ),
                        ),
                    ),
                    demoAmount = Amount(value = null, blockchain = Blockchain.Ethereum),
                    cardTokens = setOf(usdtToken),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                            UpdateWalletManagerResult.Address(
                                value = "0x11",
                                type = UpdateWalletManagerResult.Address.Type.Secondary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.ZERO),
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Token(
                                value = BigDecimal.ZERO,
                                currencyRawId = usdtToken.id?.let(CryptoCurrency::RawID),
                                contractAddress = usdtToken.contractAddress,
                            ),
                        ),
                        currentTransactions = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyTransaction.Token(
                                txInfo = createTxInfo(
                                    status = TxInfo.TransactionStatus.Unconfirmed,
                                    amount = BigDecimal.TEN,
                                ),
                                tokenId = "0x3",
                                contractAddress = "0x4",
                            ),
                        ),
                    ),
                ),
                GetDemoResultTestModel(
                    wallet = createWallet(
                        coinValue = BigDecimal.ONE,
                        tokensAmount = mapOf(usdtToken to BigDecimal.ZERO),
                        addresses = setOf(
                            Address(value = "0x1", type = AddressType.Default),
                            Address(value = "0x11", type = AddressType.Legacy),
                        ),
                        transactions = listOf(
                            createRecentTransaction(
                                amount = Amount(
                                    blockchain = Blockchain.Ethereum,
                                    value = BigDecimal.ONE,
                                ),
                                status = TransactionStatus.Unconfirmed,
                            ),
                            createRecentTransaction(
                                amount = Amount(
                                    value = BigDecimal.TEN,
                                    blockchain = Blockchain.Ethereum,
                                    type = AmountType.Token(token = usdtToken),
                                    currencySymbol = "USDT",
                                ),
                                status = TransactionStatus.Unconfirmed,
                            ),
                        ),
                    ),
                    demoAmount = Amount(value = BigDecimal.TEN, blockchain = Blockchain.Ethereum),
                    cardTokens = setOf(usdtToken),
                    expected = UpdateWalletManagerResult.Verified(
                        selectedAddress = "0x1",
                        addresses = setOf(
                            UpdateWalletManagerResult.Address(
                                value = "0x1",
                                type = UpdateWalletManagerResult.Address.Type.Primary,
                            ),
                            UpdateWalletManagerResult.Address(
                                value = "0x11",
                                type = UpdateWalletManagerResult.Address.Type.Secondary,
                            ),
                        ),
                        currenciesAmounts = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = BigDecimal.TEN),
                            UpdateWalletManagerResult.CryptoCurrencyAmount.Token(
                                value = BigDecimal.TEN,
                                currencyRawId = usdtToken.id?.let(CryptoCurrency::RawID),
                                contractAddress = usdtToken.contractAddress,
                            ),
                        ),
                        currentTransactions = setOf(
                            UpdateWalletManagerResult.CryptoCurrencyTransaction.Token(
                                txInfo = createTxInfo(
                                    status = TxInfo.TransactionStatus.Unconfirmed,
                                    amount = BigDecimal.TEN,
                                ),
                                tokenId = "0x3",
                                contractAddress = "0x4",
                            ),
                            UpdateWalletManagerResult.CryptoCurrencyTransaction.Coin(
                                txInfo = createTxInfo(
                                    status = TxInfo.TransactionStatus.Unconfirmed,
                                    amount = BigDecimal.ONE,
                                ),
                            ),
                        ),
                    ),
                ),
                // endregion
            )
        }
    }

    data class GetDemoResultTestModel(
        val wallet: Wallet,
        val demoAmount: Amount,
        val cardTokens: Set<Token>,
        val expected: UpdateWalletManagerResult.Verified,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetNoAccountResult {

        @ParameterizedTest
        @ProvideTestModels
        fun getNoAccountResult(model: GetNoAccountResultModel) {
            // Arrange
            every { walletManager.wallet } returns model.wallet

            // Act
            val actual = factory.getNoAccountResult(
                walletManager = walletManager,
                customMessage = model.customMessage,
                amountToCreateAccount = model.amountToCreateAccount,
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // region amountToCreateAccount is null
            GetNoAccountResultModel(
                wallet = createWallet(
                    coinValue = null,
                    addresses = setOf(Address(value = "0x1", type = AddressType.Default)),
                    transactions = emptyList(),
                ),
                customMessage = "",
                amountToCreateAccount = null,
                expected = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            // endregion

            // region Wallet without amount and transactions
            GetNoAccountResultModel(
                wallet = createWallet(
                    coinValue = null,
                    addresses = setOf(Address(value = "0x1", type = AddressType.Default)),
                    transactions = emptyList(),
                ),
                customMessage = "",
                amountToCreateAccount = BigDecimal.ONE,
                expected = mockFactory.createNoAccount(),
            ),
            // endregion

            // region Wallet with coin amount and without transactions
            GetNoAccountResultModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                    ),
                    transactions = emptyList(),
                ),
                customMessage = "custom message",
                amountToCreateAccount = BigDecimal.ONE,
                expected = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                    amountToCreateAccount = BigDecimal.ONE,
                    errorMessage = "custom message",
                ),
            ),
            // endregion

            // region Wallet with coin amount and coin transaction
            GetNoAccountResultModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                    ),
                    transactions = listOf(
                        createRecentTransaction(
                            amount = Amount(
                                blockchain = Blockchain.Ethereum,
                                value = BigDecimal.ONE,
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                    ),
                ),
                customMessage = "",
                amountToCreateAccount = BigDecimal.ZERO,
                expected = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                ),
            ),
            // endregion

            // region Wallet with amounts and transactions (coin and token)
            GetNoAccountResultModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    tokensAmount = mapOf(usdtToken to BigDecimal.ZERO),
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                        Address(value = "0x11", type = AddressType.Legacy),
                    ),
                    transactions = listOf(
                        createRecentTransaction(
                            amount = Amount(
                                blockchain = Blockchain.Ethereum,
                                value = BigDecimal.ONE,
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                        createRecentTransaction(
                            amount = Amount(
                                value = BigDecimal.TEN,
                                blockchain = Blockchain.Ethereum,
                                type = AmountType.Token(token = usdtToken),
                                currencySymbol = "USDT",
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                    ),
                ),
                customMessage = "",
                amountToCreateAccount = BigDecimal.ZERO,
                expected = UpdateWalletManagerResult.NoAccount(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                        UpdateWalletManagerResult.Address(
                            value = "0x11",
                            type = UpdateWalletManagerResult.Address.Type.Secondary,
                        ),
                    ),
                    amountToCreateAccount = BigDecimal.ZERO,
                    errorMessage = "",
                ),
            ),
            // endregion
        )
    }

    data class GetNoAccountResultModel(
        val wallet: Wallet,
        val customMessage: String,
        val amountToCreateAccount: BigDecimal?,
        val expected: UpdateWalletManagerResult,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetUnreachableResult {

        @ParameterizedTest
        @ProvideTestModels
        fun getUnreachableResult(model: GetUnreachableResultModel) {
            // Arrange
            every { walletManager.wallet } returns model.wallet

            // Act
            val actual = factory.getUnreachableResult(walletManager = walletManager)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // region Wallet without amount and transactions
            GetUnreachableResultModel(
                wallet = createWallet(
                    coinValue = null,
                    addresses = setOf(Address(value = "0x1", type = AddressType.Default)),
                    transactions = emptyList(),
                ),
                expected = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            // endregion

            // region Wallet with coin amount and without transactions
            GetUnreachableResultModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                    ),
                    transactions = emptyList(),
                ),
                expected = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            // endregion

            // region Wallet with coin amount and coin transaction
            GetUnreachableResultModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                    ),
                    transactions = listOf(
                        createRecentTransaction(
                            amount = Amount(
                                blockchain = Blockchain.Ethereum,
                                value = BigDecimal.ONE,
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                    ),
                ),
                expected = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                    ),
                ),
            ),
            // endregion

            // region Wallet with amounts and transactions (coin and token)
            GetUnreachableResultModel(
                wallet = createWallet(
                    coinValue = BigDecimal.ONE,
                    tokensAmount = mapOf(usdtToken to BigDecimal.ZERO),
                    addresses = setOf(
                        Address(value = "0x1", type = AddressType.Default),
                        Address(value = "0x11", type = AddressType.Legacy),
                    ),
                    transactions = listOf(
                        createRecentTransaction(
                            amount = Amount(
                                blockchain = Blockchain.Ethereum,
                                value = BigDecimal.ONE,
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                        createRecentTransaction(
                            amount = Amount(
                                value = BigDecimal.TEN,
                                blockchain = Blockchain.Ethereum,
                                type = AmountType.Token(token = usdtToken),
                                currencySymbol = "USDT",
                            ),
                            status = TransactionStatus.Unconfirmed,
                        ),
                    ),
                ),
                expected = UpdateWalletManagerResult.Unreachable(
                    selectedAddress = "0x1",
                    addresses = setOf(
                        UpdateWalletManagerResult.Address(
                            value = "0x1",
                            type = UpdateWalletManagerResult.Address.Type.Primary,
                        ),
                        UpdateWalletManagerResult.Address(
                            value = "0x11",
                            type = UpdateWalletManagerResult.Address.Type.Secondary,
                        ),
                    ),
                ),
            ),
            // endregion
        )
    }

    data class GetUnreachableResultModel(
        val wallet: Wallet,
        val expected: UpdateWalletManagerResult,
    )

    private fun createWallet(
        coinValue: BigDecimal?,
        tokensAmount: Map<Token, BigDecimal> = emptyMap(),
        addresses: Set<Address>,
        transactions: List<TransactionData.Uncompiled>,
    ): Wallet {
        return Wallet(
            blockchain = Blockchain.Ethereum,
            addresses = addresses,
            publicKey = mockk(),
            tokens = setOf(),
        ).apply {
            coinValue?.let(::setCoinValue)
            tokensAmount.forEach { (token, amount) -> addTokenValue(value = amount, token = token) }
            recentTransactions += transactions
        }
    }

    private fun createRecentTransaction(amount: Amount, status: TransactionStatus): TransactionData.Uncompiled {
        return TransactionData.Uncompiled(
            amount = amount,
            fee = null,
            sourceAddress = "0x1",
            destinationAddress = "0x2",
            status = status,
            hash = "hash",
            date = Calendar.getInstance().apply {
                timeInMillis = 1748251839317
            },
            extras = null,
            contractAddress = null,
        )
    }

    private fun createTxInfo(status: TxInfo.TransactionStatus, amount: BigDecimal): TxInfo {
        return TxInfo(
            txHash = "hash",
            timestampInMillis = 1748251839317,
            isOutgoing = true,
            destinationType = TxInfo.DestinationType.Single(
                addressType = TxInfo.AddressType.User(address = "0x2"),
            ),
            sourceType = TxInfo.SourceType.Single(address = "0x1"),
            interactionAddressType = TxInfo.InteractionAddressType.User(address = "0x2"),
            status = status,
            type = TxInfo.TransactionType.Transfer,
            amount = amount,
        )
    }

    private companion object {

        val usdtToken = Token(
            id = "0x3",
            contractAddress = "0x4",
            symbol = "USDT",
            decimals = 6,
            name = "Tether",
        )
    }
}