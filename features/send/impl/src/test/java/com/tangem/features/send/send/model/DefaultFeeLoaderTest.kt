package com.tangem.features.send.send.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultFeeLoaderTest {

    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk()

    private val userWallet: UserWallet = mockk()
    private val network = trx.network
    private val transactionData: TransactionData = mockk()

    private lateinit var loader: DefaultFeeLoader

    @BeforeEach
    fun setUp() {
        clearMocks(getFeeForGaslessUseCase, getFeeUseCase)
        coEvery { getFeeUseCase(userWallet, network, transactionData) } returns plainNativeFee.right()
        loader = DefaultFeeLoader(getFeeForGaslessUseCase, getFeeUseCase)
    }

    @Test
    fun `GIVEN the coin is preferred over a compensation fee WHEN loading again THEN native fee stays pinned`() =
        runTest {
            // Arrange
            givenDefaultFee(fee = nativeFee.right(), isNativeFeePreferred = true)

            // Act
            val first = load()
            val second = load()

            // Assert
            assertThat(first).isEqualTo(nativeFee.right())
            assertThat(second).isEqualTo(
                TransactionFeeExtended(transactionFee = plainNativeFee, feeTokenId = trx.id, nativeFee = plainNativeFee)
                    .right(),
            )
            coVerify(exactly = 1) {
                getFeeForGaslessUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    sentAmount = any(),
                    onNativeFeePreferred = any(),
                )
            }
            coVerify(exactly = 1) { getFeeUseCase(userWallet, network, transactionData) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the coin is not preferred over a compensation fee WHEN loading again THEN default fee is resolved again`(
        fee: Either<GetFeeError, TransactionFeeExtended>,
    ) = runTest {
        // Arrange
        givenDefaultFee(fee = fee, isNativeFeePreferred = false)

        // Act
        val first = load()
        val second = load()

        // Assert
        assertThat(first).isEqualTo(fee)
        assertThat(second).isEqualTo(fee)
        coVerify(exactly = 2) {
            getFeeForGaslessUseCase(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
                sentAmount = SENT_AMOUNT,
                onNativeFeePreferred = any(),
            )
        }
        verify { getFeeUseCase wasNot Called }
    }

    @Test
    fun `GIVEN the coin is preferred with a fee of another coin id WHEN loading again THEN pinned fee keeps that id`() =
        runTest {
            // Arrange
            val secondaryAccountTrxId = CryptoCurrency.ID.fromValue("coin⟨TRON→42⟩tron")
            givenDefaultFee(fee = nativeFee.copy(feeTokenId = secondaryAccountTrxId).right(), isNativeFeePreferred = true)

            // Act
            load()
            val second = load()

            // Assert
            assertThat(second).isEqualTo(
                TransactionFeeExtended(
                    transactionFee = plainNativeFee,
                    feeTokenId = secondaryAccountTrxId,
                    nativeFee = plainNativeFee,
                ).right(),
            )
        }

    @Test
    fun `GIVEN the coin is preferred but the fee fails WHEN loading again THEN default fee is resolved again`() =
        runTest {
            // Arrange
            givenDefaultFee(fee = GetFeeError.UnknownError.left(), isNativeFeePreferred = true)

            // Act
            load()
            load()

            // Assert
            coVerify(exactly = 2) {
                getFeeForGaslessUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    sentAmount = any(),
                    onNativeFeePreferred = any(),
                )
            }
            verify { getFeeUseCase wasNot Called }
        }

    private fun givenDefaultFee(fee: Either<GetFeeError, TransactionFeeExtended>, isNativeFeePreferred: Boolean) {
        coEvery {
            getFeeForGaslessUseCase(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
                sentAmount = SENT_AMOUNT,
                onNativeFeePreferred = any(),
            )
        } answers {
            if (isNativeFeePreferred) arg<() -> Unit>(4).invoke()
            fee
        }
    }

    private suspend fun load(): Either<GetFeeError, TransactionFeeExtended> = loader.load(
        userWallet = userWallet,
        network = network,
        transactionData = transactionData,
        sentAmount = SENT_AMOUNT,
    )

    private fun provideTestModels() = listOf(
        // A coin fee never weighed against a compensation fee: EVM network or an unsupported token
        nativeFee.right(),
        gaslessFee.right(),
        GetFeeError.UnknownError.left(),
    )

    private companion object {
        val SENT_AMOUNT: BigDecimal = BigDecimal("100")

        val currencyFactory = MockCryptoCurrencyFactory()
        val trx: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Tron)
        val usdt: CryptoCurrency.Token = currencyFactory.createToken(blockchain = Blockchain.Tron, id = "usdt")

        val plainNativeFee: TransactionFee = TransactionFee.Single(
            normal = Fee.Common(Amount(BigDecimal("7.5"), Blockchain.Tron)),
        )

        val nativeFee = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal("6.43"), Blockchain.Tron))),
            feeTokenId = trx.id,
        )

        val gaslessFee = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal("2.41"), Blockchain.Tron))),
            feeTokenId = usdt.id,
        )
    }
}