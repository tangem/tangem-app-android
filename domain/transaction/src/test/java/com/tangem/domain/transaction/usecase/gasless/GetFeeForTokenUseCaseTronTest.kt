package com.tangem.domain.transaction.usecase.gasless

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/** Explicit fee-token selection on Tron. */
internal class GetFeeForTokenUseCaseTronTest {

    private val currencyChecksRepository: CurrencyChecksRepository = mockk(relaxed = true)
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase = mockk()
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase = mockk()

    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val usdt: CryptoCurrency.Token = currencyFactory.createToken(
        blockchain = Blockchain.Tron,
        contractAddress = USDT_CONTRACT,
    )

    private val transferTransaction = TransactionData.Uncompiled(
        amount = Amount(
            token = Token(symbol = "USDT", contractAddress = USDT_CONTRACT, decimals = 6),
            value = BigDecimal("50"),
        ),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TTo",
    )

    private val tronGaslessFee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(
                amount = Amount(
                    token = Token(symbol = "USDT", contractAddress = USDT_CONTRACT, decimals = 6),
                    value = BigDecimal("2.75"),
                ),
            ),
        ),
        feeTokenId = usdt.id,
        gaslessProtocolData = GaslessProtocolData.Tron(quote = QUOTE),
    )

    private val useCase = GetFeeForTokenUseCase(
        singleAccountStatusListSupplier = mockk(),
        currencyChecksRepository = currencyChecksRepository,
        evmGaslessFeeResolver = mockk(),
        tronGaslessFeeResolver = TronGaslessFeeResolver(
            getTronGaslessFeeUseCase = getTronGaslessFeeUseCase,
            isTronGaslessSupportedUseCase = isTronGaslessSupportedUseCase,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(currencyChecksRepository, getTronGaslessFeeUseCase, isTronGaslessSupportedUseCase)
    }

    @Test
    fun `GIVEN a supported Tron token WHEN invoke THEN the compensation quote is returned`() = runTest {
        // Arrange
        coEvery { isTronGaslessSupportedUseCase(userWalletId, usdt.network, usdt) } returns true
        coEvery { getTronGaslessFeeUseCase(userWalletId, transferTransaction, usdt) } returns tronGaslessFee.right()

        // Act
        val actual = useCase(userWallet, usdt, transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(tronGaslessFee.right())
    }

    @Test
    fun `GIVEN the quote fails WHEN invoke THEN the error is surfaced`() = runTest {
        // Arrange — the token was picked explicitly, so there is no fee to fall back to.
        coEvery { isTronGaslessSupportedUseCase(userWalletId, usdt.network, usdt) } returns true
        coEvery { getTronGaslessFeeUseCase(userWalletId, transferTransaction, usdt) } returns
            GetFeeError.GaslessError.NotEnoughFunds.left()

        // Act
        val actual = useCase(userWallet, usdt, transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(GetFeeError.GaslessError.NotEnoughFunds.left())
    }

    @Test
    fun `GIVEN an unsupported Tron token WHEN invoke THEN the gasless service rejects the network`() = runTest {
        // Arrange
        coEvery { isTronGaslessSupportedUseCase(userWalletId, usdt.network, usdt) } returns false
        every { currencyChecksRepository.isNetworkSupportedForGaslessTx(usdt.network) } returns false

        // Act
        val actual = useCase(userWallet, usdt, transferTransaction)

        // Assert
        assertThat(actual).isEqualTo(GetFeeError.GaslessError.NetworkIsNotSupported.left())
        coVerify(exactly = 0) { getTronGaslessFeeUseCase(any(), any(), any()) }
    }

    private companion object {
        const val USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"

        val QUOTE = TronGaslessQuote(
            quoteId = "q_1",
            quotedAmountRaw = BigInteger("50000000"),
            quotedToAddress = "TTo",
            feeRecipient = "TFee",
            compensationToken = USDT_CONTRACT,
            compensationAmountRaw = BigInteger("2750000"),
            compensationAmountDecimal = BigDecimal("2.75"),
            energy = 78_000,
            bandwidth = 345,
            trxCost = BigDecimal("8.0722"),
            expiresAtEpochMs = 1_780_489_496_000,
        )
    }
}