package com.tangem.domain.transaction.usecase.gasless

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateAndSendEvmGaslessFeeTokenTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()

    private val sut = CreateAndSendEvmGaslessTransactionUseCase(
        walletManagersFacade = walletManagersFacade,
        singleAccountListSupplier = singleAccountListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
        gaslessTransactionRepository = mockk(),
        gaslessSignerProvider = mockk(),
    )

    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }
    private val accountUsdc = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Polygon,
        contractAddress = USDC_CONTRACT,
        symbol = "USDC",
        decimals = 6,
    )
    private val transactionData = TransactionData.Uncompiled(
        amount = Amount(
            token = Token(symbol = "USDC", contractAddress = USDC_CONTRACT, decimals = 6),
            value = BigDecimal("10"),
        ),
        fee = null,
        sourceAddress = "0xaccount",
        destinationAddress = "0xrecipient",
    )
    private val fee = TransactionFeeExtended(transactionFee = mockk(), feeTokenId = accountUsdc.id)

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, singleAccountListSupplier, nonPortfolioCurrenciesProvider)
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, accountUsdc.network) } returns null
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a fee token outside the portfolio WHEN invoke THEN it is looked up in non-portfolio accounts`(
        model: FeeTokenModel,
    ) = runTest {
        // Arrange
        coEvery { nonPortfolioCurrenciesProvider.findCurrency(userWalletId, accountUsdc.id) } returns
            model.accountToken

        // Act
        val actual = sut(userWallet = userWallet, transactionData = transactionData, fee = fee)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    private fun provideTestModels(): List<FeeTokenModel> = listOf(
        FeeTokenModel(
            accountToken = accountUsdc,
            expected = SendTransactionError.DataError("WalletManager not found for network ${accountUsdc.network.id}"),
        ),
        FeeTokenModel(accountToken = null, expected = SendTransactionError.DataError("Token for fee not found")),
    )

    data class FeeTokenModel(val accountToken: CryptoCurrency?, val expected: SendTransactionError)

    private companion object {
        const val USDC_CONTRACT = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
    }
}