package com.tangem.features.yield.supply.impl.common

import arrow.core.Option
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendEvmGaslessTransactionUseCase
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class YieldSupplySmartGasTest : YieldSupplyActionModelTestBase() {

    private val createAndSendEvmGaslessTransactionUseCase: CreateAndSendEvmGaslessTransactionUseCase = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val accountStatusList: AccountStatusList = mockk()
    private val gaslessFee: TransactionFeeExtended = mockk()

    private val yieldSupplySmartGas = YieldSupplySmartGas(
        sendTransactionUseCase = sendTransactionUseCase,
        createAndSendEvmGaslessTransactionUseCase = createAndSendEvmGaslessTransactionUseCase,
        singleAccountStatusListSupplier = accountStatusListSupplier,
    )

    private val transactions = listOf(uncompiledTx(), uncompiledTx())

    @BeforeEach
    fun resetMocks() {
        clearMocks(sendTransactionUseCase, createAndSendEvmGaslessTransactionUseCase, accountStatusListSupplier)
    }

    @Nested
    inner class SendTransactions {

        @Test
        fun `GIVEN Smart Gas fee WHEN sendTransactions THEN batch goes as one gasless transaction`() = runTest {
            // Arrange
            coEvery {
                createAndSendEvmGaslessTransactionUseCase(userWallet, transactions, gaslessFee)
            } returns GASLESS_HASH.right()

            // Act
            val actual = yieldSupplySmartGas.sendTransactions(
                userWallet = userWallet,
                network = token.network,
                yieldSupplyFeeUM = feeUM(gaslessFee = gaslessFee),
            )

            // Assert
            assertThat(actual).isEqualTo(listOf(GASLESS_HASH).right())
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN gasless send fails WHEN sendTransactions THEN returns its error`() = runTest {
            // Arrange
            val error = SendTransactionError.UserCancelledError
            coEvery {
                createAndSendEvmGaslessTransactionUseCase(userWallet, transactions, gaslessFee)
            } returns error.left()

            // Act
            val actual = yieldSupplySmartGas.sendTransactions(
                userWallet = userWallet,
                network = token.network,
                yieldSupplyFeeUM = feeUM(gaslessFee = gaslessFee),
            )

            // Assert
            assertThat(actual).isEqualTo(error.left())
        }

        @Test
        fun `GIVEN native fee WHEN sendTransactions THEN each transaction is sent with the coin fee`() = runTest {
            // Arrange
            val hashes = listOf("0xfirst", "0xsecond")
            coEvery {
                sendTransactionUseCase(
                    txsData = transactions,
                    userWallet = userWallet,
                    network = token.network,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            } returns hashes.right()

            // Act
            val actual = yieldSupplySmartGas.sendTransactions(
                userWallet = userWallet,
                network = token.network,
                yieldSupplyFeeUM = feeUM(gaslessFee = null),
            )

            // Assert
            assertThat(actual).isEqualTo(hashes.right())
            coVerify(exactly = 0) {
                createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any())
            }
        }
    }

    @Nested
    inner class GetFeeTokenStatus {

        @BeforeEach
        fun setUp() {
            mockkObject(CryptoCurrencyStatusOperations)
        }

        @AfterEach
        fun tearDown() {
            unmockkObject(CryptoCurrencyStatusOperations)
        }

        @Test
        fun `GIVEN fee token in portfolio WHEN getFeeTokenStatus THEN returns its status`() = runTest {
            // Arrange
            coEvery { accountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
            stubFeeTokenLookup(cryptoCurrencyStatus.some())

            // Act
            val actual = yieldSupplySmartGas.getFeeTokenStatus(userWalletId, token.id, token.network)

            // Assert
            assertThat(actual).isEqualTo(cryptoCurrencyStatus)
        }

        @Test
        fun `GIVEN fee token not in portfolio WHEN getFeeTokenStatus THEN returns null`() = runTest {
            // Arrange
            coEvery { accountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
            stubFeeTokenLookup(none())

            // Act
            val actual = yieldSupplySmartGas.getFeeTokenStatus(userWalletId, token.id, token.network)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN no account list WHEN getFeeTokenStatus THEN returns null`() = runTest {
            // Arrange
            coEvery { accountStatusListSupplier.getSyncOrNull(userWalletId) } returns null

            // Act
            val actual = yieldSupplySmartGas.getFeeTokenStatus(userWalletId, token.id, token.network)

            // Assert
            assertThat(actual).isNull()
        }

        private fun stubFeeTokenLookup(result: Option<CryptoCurrencyStatus>) {
            every {
                with(CryptoCurrencyStatusOperations) {
                    accountStatusList.getCryptoCurrencyStatus(token.id, any<Network>())
                }
            } returns result
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FeeAssetType {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fee and its currency WHEN feeAssetType THEN Coin only for a native coin fee`(
            model: FeeAssetTypeModel,
        ) {
            // Act
            val actual = feeUM(gaslessFee = model.gaslessFee).feeAssetType(model.feeCurrency)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FeeAssetTypeModel(gaslessFee = null, feeCurrency = coin, expected = AnalyticsParam.FeeAssetType.Coin),
            FeeAssetTypeModel(gaslessFee = null, feeCurrency = token, expected = AnalyticsParam.FeeAssetType.Token),
            FeeAssetTypeModel(
                gaslessFee = gaslessFee,
                feeCurrency = coin,
                expected = AnalyticsParam.FeeAssetType.Token,
            ),
            FeeAssetTypeModel(
                gaslessFee = gaslessFee,
                feeCurrency = token,
                expected = AnalyticsParam.FeeAssetType.Token,
            ),
        )
    }

    data class FeeAssetTypeModel(
        val gaslessFee: TransactionFeeExtended?,
        val feeCurrency: CryptoCurrency,
        val expected: AnalyticsParam.FeeAssetType,
    )

    private fun feeUM(gaslessFee: TransactionFeeExtended?) = YieldSupplyFeeUM.Content(
        transactionDataList = persistentListOf(*transactions.toTypedArray()),
        feeFiatValue = TextReference.EMPTY,
        estimatedFiatValue = TextReference.EMPTY,
        maxNetworkFeeFiatValue = TextReference.EMPTY,
        minTopUpFiatValue = TextReference.EMPTY,
        gaslessFee = gaslessFee,
    )

    private companion object {
        const val GASLESS_HASH = "0xgasless"
    }
}