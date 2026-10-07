package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SavedSwapTransactionListConverterTest {

    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk()
    private val networkFactory: NetworkFactory = mockk()

    private val converter = SavedSwapTransactionListConverter(
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        networkFactory = networkFactory,
    )

    private val userWallet = MockUserWalletFactory.create(
        scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap()),
    )
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory(userWallet)
    private val ethereum = cryptoCurrencyFactory.ethereum
    private val usdt = cryptoCurrencyFactory.createToken(
        blockchain = Blockchain.Ethereum,
        id = "tether",
        contractAddress = USDT_CONTRACT_ADDRESS,
        symbol = "USDT",
        decimals = 6,
    )
    private val mainAccount = Account.Personal.createMainAccount(userWallet.walletId)
    private val paymentAccount = Account.Payment(userWallet.walletId)
    private val accountList = (AccountList.empty(userWallet.walletId) + paymentAccount).getOrNull()!!
    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val transaction = SavedSwapTransactionModel(
        txId = "tx-1",
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
    )

    @BeforeEach
    fun setUp() {
        clearMocks(responseCryptoCurrenciesFactory, networkFactory)

        every {
            networkFactory.create(
                blockchain = any(),
                extraDerivationPath = any(),
                userWallet = any(),
                accountIndex = any(),
            )
        } returns ethereum.network
        every {
            networkFactory.create(blockchain = any(), derivationPath = any(), userWallet = any())
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

    @Test
    fun `GIVEN payment accountId on main derivation path WHEN convertBack THEN account is payment`() {
        // Arrange
        val value = createSavedTransactions(
            fromAccountId = paymentAccount.accountId.value,
            toAccountId = mainAccount.accountId.value,
        )

        // Act
        val actual = converter.convertBack(
            value = value,
            accountList = accountList,
            userWallet = userWallet,
            txStatuses = emptyMap(),
        )

        // Assert
        assertThat(actual).isEqualTo(createExpected(fromAccount = paymentAccount, toAccount = mainAccount))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN accountId not in account list WHEN convertBack THEN account resolved by derivation index`(
        model: FallbackModel,
    ) {
        // Arrange
        val value = createSavedTransactions(
            fromAccountId = model.accountId,
            toAccountId = model.accountId,
            isDerivationPathStored = model.isDerivationPathStored,
        )

        // Act
        val actual = converter.convertBack(
            value = value,
            accountList = accountList,
            userWallet = userWallet,
            txStatuses = emptyMap(),
        )

        // Assert
        assertThat(actual).isEqualTo(
            createExpected(fromAccount = model.expectedAccount, toAccount = model.expectedAccount),
        )
    }

    private fun provideTestModels() = listOf(
        FallbackModel(accountId = null, isDerivationPathStored = true, expectedAccount = mainAccount),
        FallbackModel(accountId = UNKNOWN_ACCOUNT_ID, isDerivationPathStored = true, expectedAccount = mainAccount),
        FallbackModel(accountId = null, isDerivationPathStored = false, expectedAccount = paymentAccount),
    )

    private fun createSavedTransactions(
        fromAccountId: String?,
        toAccountId: String?,
        isDerivationPathStored: Boolean = true,
    ) = SavedSwapTransactionListModelInner(
        fromUserWalletId = userWallet.walletId.stringValue,
        toUserWalletId = userWallet.walletId.stringValue,
        fromCryptoCurrencyId = ethereum.id.value,
        toCryptoCurrencyId = usdt.id.value,
        fromTokensResponse = createResponseToken(ethereum, fromAccountId, isDerivationPathStored),
        toTokensResponse = createResponseToken(usdt, toAccountId, isDerivationPathStored),
        transactions = listOf(transaction),
    )

    private fun createResponseToken(
        currency: CryptoCurrency,
        accountId: String?,
        isDerivationPathStored: Boolean,
    ): UserTokensResponse.Token {
        val token = userTokensResponseFactory.createResponseToken(currency).copy(accountId = accountId)

        return if (isDerivationPathStored) token else token.copy(derivationPath = null)
    }

    private fun createExpected(fromAccount: Account, toAccount: Account) = SavedSwapTransactionListModel(
        fromUserWalletId = userWallet.walletId.stringValue,
        toUserWalletId = userWallet.walletId.stringValue,
        fromCryptoCurrencyId = ethereum.id.value,
        toCryptoCurrencyId = usdt.id.value,
        fromCryptoCurrency = ethereum,
        toCryptoCurrency = usdt,
        fromAccount = fromAccount,
        toAccount = toAccount,
        transactions = listOf(transaction),
    )

    data class FallbackModel(
        val accountId: String?,
        val isDerivationPathStored: Boolean,
        val expectedAccount: Account,
    )

    private companion object {
        const val USDT_CONTRACT_ADDRESS = "0xdac17f958d2ee523a2206206994597c13d831ec7"
        const val UNKNOWN_ACCOUNT_ID = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    }
}