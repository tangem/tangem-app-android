package com.tangem.feature.swap

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.domain.SwapTxTypeDTO
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class DefaultSwapTransactionRepositoryTest {

    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk()
    private val networkFactory: NetworkFactory = mockk()
    private val growPreferencesKeys = GrowPreferencesKeys()
    private val appPreferencesStore = createAppPreferencesStore(
        moshi = Moshi.Builder()
            .add(BigDecimalAdapter())
            .add(DateTimeAdapter())
            .build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )

    private val repository = DefaultSwapTransactionRepository(
        appPreferencesStore = appPreferencesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
        singleAccountListSupplier = singleAccountListSupplier,
        growPreferencesKeys = growPreferencesKeys,
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        networkFactory = networkFactory,
    )

    private val userWallet = MockUserWalletFactory.create(
        scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
    )
    private val walletId = userWallet.walletId
    private val otherWalletId = UserWalletId(stringValue = "ab12")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory(userWallet)
    private val ethereum = cryptoCurrencyFactory.ethereum
    private val usdt = cryptoCurrencyFactory.createToken(
        blockchain = Blockchain.Ethereum,
        id = "tether",
        contractAddress = USDT_CONTRACT_ADDRESS,
        symbol = "USDT",
        decimals = 6,
    )
    private val mainAccount = Account.Personal.createMainAccount(walletId)
    private val paymentAccount = Account.Payment(walletId)
    private val accountList = (AccountList.empty(walletId) + paymentAccount).getOrNull()!!
    private val userTokensResponseFactory = UserTokensResponseFactory()

    @BeforeEach
    fun setUp() = runTest {
        clearMocks(singleAccountListSupplier, responseCryptoCurrenciesFactory, networkFactory)
        appPreferencesStore.editData { it.clear() }

        every {
            singleAccountListSupplier(params = any<SingleAccountListProducer.Params>())
        } returns flowOf(accountList)
        every {
            networkFactory.create(
                blockchain = any(),
                extraDerivationPath = any(),
                userWallet = any(),
                accountIndex = any(),
            )
        } returns ethereum.network
        every {
            responseCryptoCurrenciesFactory.createCurrency(
                responseToken = match { it.contractAddress == null },
                userWallet = any(),
                network = any(),
            )
        } returns ethereum
        every {
            responseCryptoCurrenciesFactory.createCurrency(
                responseToken = match { it.contractAddress == USDT_CONTRACT_ADDRESS },
                userWallet = any(),
                network = any(),
            )
        } returns usdt
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StoreTransaction {

        @Test
        fun `GIVEN main to main group WHEN store main to payment with same pair THEN two groups stored`() = runTest {
            // Arrange
            val mainToMainTx = createTransaction(txId = "tx-1")
            val mainToPaymentTx = createTransaction(txId = "tx-2")
            store(transaction = mainToMainTx, fromAccount = mainAccount, toAccount = mainAccount)

            // Act
            store(transaction = mainToPaymentTx, fromAccount = mainAccount, toAccount = paymentAccount)

            // Assert
            val actual = repository.getTransactions(userWallet = userWallet, cryptoCurrencyId = ethereum.id).first()
            assertThat(actual).containsExactly(
                createGroup(fromAccount = mainAccount, toAccount = mainAccount, transactions = listOf(mainToMainTx)),
                createGroup(
                    fromAccount = mainAccount,
                    toAccount = paymentAccount,
                    transactions = listOf(mainToPaymentTx),
                ),
            ).inOrder()
        }

        @Test
        fun `GIVEN group of same accounts WHEN store another txId THEN one group with two txs`() = runTest {
            // Arrange
            val firstTx = createTransaction(txId = "tx-1")
            val secondTx = createTransaction(txId = "tx-2")
            store(transaction = firstTx, fromAccount = mainAccount, toAccount = paymentAccount)

            // Act
            store(transaction = secondTx, fromAccount = mainAccount, toAccount = paymentAccount)

            // Assert
            val actual = repository.getTransactions(userWallet = userWallet, cryptoCurrencyId = ethereum.id).first()
            assertThat(actual).containsExactly(
                createGroup(
                    fromAccount = mainAccount,
                    toAccount = paymentAccount,
                    transactions = listOf(firstTx, secondTx),
                ),
            )
        }

        @Test
        fun `GIVEN legacy group without accountIds WHEN store with accounts THEN legacy untouched and new group added`() =
            runTest {
                // Arrange
                val legacyTx = createTransaction(txId = "tx-1")
                val newTx = createTransaction(txId = "tx-2")
                val legacyGroup = createStoredGroup(fromAccount = null, toAccount = null, transaction = legacyTx)
                appPreferencesStore.editData { preferences ->
                    preferences.setObjectList(key = growPreferencesKeys.swapTransactionsKey, value = listOf(legacyGroup))
                }

                // Act
                store(transaction = newTx, fromAccount = mainAccount, toAccount = mainAccount)

                // Assert
                val actual = appPreferencesStore.getObjectListSync<SavedSwapTransactionListModelInner>(
                    key = growPreferencesKeys.swapTransactionsKey,
                )
                assertThat(actual).containsExactly(
                    legacyGroup,
                    createStoredGroup(fromAccount = mainAccount, toAccount = mainAccount, transaction = newTx),
                ).inOrder()
                assertThat(actual.map { it.fromTokensResponse?.accountId to it.toTokensResponse?.accountId })
                    .containsExactly(
                        null to null,
                        mainAccount.accountId.value to mainAccount.accountId.value,
                    ).inOrder()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTransactions {

        @Test
        fun `GIVEN pair split into account groups WHEN getTransactions by to currency THEN swaps of both returned`() =
            runTest {
                // Arrange
                val mainToMainTx = createTransaction(txId = "tx-1")
                val mainToPaymentTx = createTransaction(txId = "tx-2")
                val sendWithSwapTx = createTransaction(txId = "tx-3", type = SwapTxTypeDTO.SendWithSwap)
                store(transaction = mainToMainTx, fromAccount = mainAccount, toAccount = mainAccount)
                store(transaction = mainToPaymentTx, fromAccount = mainAccount, toAccount = paymentAccount)
                store(transaction = sendWithSwapTx, fromAccount = mainAccount, toAccount = paymentAccount)

                // Act
                val actual = repository.getTransactions(userWallet = userWallet, cryptoCurrencyId = usdt.id).first()

                // Assert
                assertThat(actual).containsExactly(
                    createGroup(
                        fromAccount = mainAccount,
                        toAccount = mainAccount,
                        transactions = listOf(mainToMainTx),
                    ),
                    createGroup(
                        fromAccount = mainAccount,
                        toAccount = paymentAccount,
                        transactions = listOf(mainToPaymentTx),
                    ),
                ).inOrder()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountTransactions {

        @Test
        fun `GIVEN payment account is sender WHEN getAccountTransactions THEN all group txs returned`() = runTest {
            // Arrange
            val swapTx = createTransaction(txId = "tx-1")
            val sendWithSwapTx = createTransaction(txId = "tx-2", type = SwapTxTypeDTO.SendWithSwap)
            store(transaction = swapTx, fromAccount = paymentAccount, toAccount = mainAccount)
            store(transaction = sendWithSwapTx, fromAccount = paymentAccount, toAccount = mainAccount)

            // Act
            val actual = repository.getAccountTransactions(
                userWallet = userWallet,
                accountId = paymentAccount.accountId,
            ).first()

            // Assert
            assertThat(actual).containsExactly(
                createGroup(
                    fromAccount = paymentAccount,
                    toAccount = mainAccount,
                    transactions = listOf(swapTx, sendWithSwapTx),
                ),
            )
        }

        @Test
        fun `GIVEN payment account is receiver WHEN getAccountTransactions THEN only swap txs returned`() = runTest {
            // Arrange
            val swapTx = createTransaction(txId = "tx-1")
            val sendWithSwapTx = createTransaction(txId = "tx-2", type = SwapTxTypeDTO.SendWithSwap)
            store(transaction = swapTx, fromAccount = mainAccount, toAccount = paymentAccount)
            store(transaction = sendWithSwapTx, fromAccount = mainAccount, toAccount = paymentAccount)

            // Act
            val actual = repository.getAccountTransactions(
                userWallet = userWallet,
                accountId = paymentAccount.accountId,
            ).first()

            // Assert
            assertThat(actual).containsExactly(
                createGroup(fromAccount = mainAccount, toAccount = paymentAccount, transactions = listOf(swapTx)),
            )
        }

        @Test
        fun `GIVEN group without payment account WHEN getAccountTransactions THEN it is excluded`() = runTest {
            // Arrange
            val mainToMainTx = createTransaction(txId = "tx-1")
            val mainToPaymentTx = createTransaction(txId = "tx-2")
            store(transaction = mainToMainTx, fromAccount = mainAccount, toAccount = mainAccount)
            store(transaction = mainToPaymentTx, fromAccount = mainAccount, toAccount = paymentAccount)

            // Act
            val actual = repository.getAccountTransactions(
                userWallet = userWallet,
                accountId = paymentAccount.accountId,
            ).first()

            // Assert
            assertThat(actual).containsExactly(
                createGroup(
                    fromAccount = mainAccount,
                    toAccount = paymentAccount,
                    transactions = listOf(mainToPaymentTx),
                ),
            )
        }

        @Test
        fun `GIVEN other wallet group with same accountId WHEN getAccountTransactions THEN it is excluded`() =
            runTest {
                // Arrange
                val otherWalletTx = createTransaction(txId = "tx-1")
                val ownWalletTx = createTransaction(txId = "tx-2")
                store(
                    transaction = otherWalletTx,
                    fromAccount = paymentAccount,
                    toAccount = paymentAccount,
                    userWalletId = otherWalletId,
                )
                store(transaction = ownWalletTx, fromAccount = paymentAccount, toAccount = mainAccount)

                // Act
                val actual = repository.getAccountTransactions(
                    userWallet = userWallet,
                    accountId = paymentAccount.accountId,
                ).first()

                // Assert
                assertThat(actual).containsExactly(
                    createGroup(
                        fromAccount = paymentAccount,
                        toAccount = mainAccount,
                        transactions = listOf(ownWalletTx),
                    ),
                )
            }

        @Test
        fun `GIVEN nothing stored WHEN getAccountTransactions THEN returns empty list`() = runTest {
            // Act
            val actual = repository.getAccountTransactions(
                userWallet = userWallet,
                accountId = paymentAccount.accountId,
            ).first()

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    private suspend fun store(
        transaction: SavedSwapTransactionModel,
        fromAccount: Account,
        toAccount: Account,
        userWalletId: UserWalletId = walletId,
    ) {
        repository.storeTransaction(
            fromUserWalletId = userWalletId,
            toUserWalletId = userWalletId,
            fromCryptoCurrency = ethereum,
            toCryptoCurrency = usdt,
            fromAccount = fromAccount,
            toAccount = toAccount,
            transaction = transaction,
        )
    }

    private fun createTransaction(txId: String, type: SwapTxTypeDTO = SwapTxTypeDTO.Swap) = SavedSwapTransactionModel(
        txId = txId,
        timestamp = 1L,
        fromCryptoAmount = BigDecimal.ONE,
        toCryptoAmount = BigDecimal.TEN,
        provider = SwapProvider(
            providerId = "changelly",
            name = "Changelly",
            type = ExchangeProviderType.CEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        ),
        swapTxTypeDTO = type,
    )

    private fun createStoredGroup(fromAccount: Account?, toAccount: Account?, transaction: SavedSwapTransactionModel) =
        SavedSwapTransactionListModelInner(
            fromUserWalletId = walletId.stringValue,
            toUserWalletId = walletId.stringValue,
            fromCryptoCurrencyId = ethereum.id.value,
            toCryptoCurrencyId = usdt.id.value,
            fromTokensResponse = userTokensResponseFactory.createResponseToken(ethereum, fromAccount?.accountId),
            toTokensResponse = userTokensResponseFactory.createResponseToken(usdt, toAccount?.accountId),
            transactions = listOf(transaction),
        )

    private fun createGroup(
        fromAccount: Account,
        toAccount: Account,
        transactions: List<SavedSwapTransactionModel>,
    ) = SavedSwapTransactionListModel(
        fromUserWalletId = walletId.stringValue,
        toUserWalletId = walletId.stringValue,
        fromCryptoCurrencyId = ethereum.id.value,
        toCryptoCurrencyId = usdt.id.value,
        fromCryptoCurrency = ethereum,
        toCryptoCurrency = usdt,
        fromAccount = fromAccount,
        toAccount = toAccount,
        transactions = transactions,
    )

    private class BigDecimalAdapter {
        @ToJson
        fun toJson(value: BigDecimal): String = value.toPlainString()

        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)
    }

    private class DateTimeAdapter {
        @ToJson
        fun toJson(value: DateTime): String = value.toString()

        @FromJson
        fun fromJson(value: String): DateTime = DateTime.parse(value)
    }

    private companion object {
        const val USDT_CONTRACT_ADDRESS = "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }
}