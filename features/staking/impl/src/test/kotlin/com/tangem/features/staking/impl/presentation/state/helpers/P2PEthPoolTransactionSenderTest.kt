package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.CheckStakingTransactionUseCase
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.txhistory.models.TxStatusError
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class P2PEthPoolTransactionSenderTest {

    private val transactionCreator: P2PEthPoolTransactionCreator = mockk()
    private val balanceUpdaterFactory: StakingBalanceUpdater.Factory = mockk()
    private val balanceUpdater: StakingBalanceUpdater = mockk(relaxed = true)
    private val prepareForSendUseCase: PrepareForSendUseCase = mockk()
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk()
    private val repository: P2PEthPoolRepository = mockk()
    private val checkStakingTransactionUseCase: CheckStakingTransactionUseCase = mockk()
    private val integration = p2pIntegration()

    private val broadcastResult = P2PEthPoolBroadcastResult(
        hash = "0xhash",
        status = "success",
        blockNumber = null,
        transactionIndex = null,
        gasUsed = null,
        cumulativeGasUsed = null,
        effectiveGasPrice = null,
        from = SOURCE_ADDRESS,
        to = "0xto",
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            transactionCreator,
            balanceUpdaterFactory,
            balanceUpdater,
            prepareForSendUseCase,
            getExplorerTransactionUrlUseCase,
            repository,
            checkStakingTransactionUseCase,
        )
        every { transactionCreator.extractParams(any()) } returns P2PEthPoolTransactionCreator.TransactionParams(
            actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
            amount = BigDecimal.ONE,
            vaultAddress = MockStaking.VAULT_ADDRESS,
            sourceAddress = SOURCE_ADDRESS,
        )
        coEvery { transactionCreator.createTransaction(any()) } returns unsignedTx().right()
        coEvery {
            checkStakingTransactionUseCase(network = any(), accountAddress = any(), unsignedTransaction = any(), token = any(), blockchain = any(), provider = any())
        } returns StakingTransactionVerdict.SAFE
        coEvery {
            prepareForSendUseCase(transactionData = any<TransactionData>(), userWallet = any(), network = any())
        } returns byteArrayOf(0x0A, 0xBC.toByte()).right()
        coEvery { repository.broadcastTransaction(any(), any()) } returns broadcastResult.right()
        every {
            getExplorerTransactionUrlUseCase(txHash = any(), currency = any())
        } returns "https://etherscan.io/tx/0xhash".right()
        every { balanceUpdaterFactory.create(any(), any(), any()) } returns balanceUpdater
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Validate {

        @Test
        fun `GIVEN params missing WHEN validate THEN SAFE and no security check`() = runTest {
            // Arrange
            every { transactionCreator.extractParams(any()) } returns null

            // Act
            val actual = createSender().validate()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(
                exactly = 0,
            ) { checkStakingTransactionUseCase(
                network = any(),
                accountAddress = any(),
                unsignedTransaction = any(),
                token = any(),
                blockchain = any(),
                provider = any(),
            ) }
        }

        @Test
        fun `GIVEN creator left WHEN validate THEN SAFE and no security check`() = runTest {
            // Arrange
            coEvery { transactionCreator.createTransaction(any()) } returns StakingError.DomainError("x").left()

            // Act
            val actual = createSender().validate()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(
                exactly = 0,
            ) { checkStakingTransactionUseCase(
                network = any(),
                accountAddress = any(),
                unsignedTransaction = any(),
                token = any(),
                blockchain = any(),
                provider = any(),
            ) }
        }

        @Test
        fun `GIVEN built transaction WHEN validate THEN verdict from security check with ethereum compiled json`() =
            runTest {
                // Arrange
                coEvery {
                    checkStakingTransactionUseCase(network = any(), accountAddress = any(), unsignedTransaction = any(), token = any(), blockchain = any(), provider = any())
                } returns StakingTransactionVerdict.WARNING

                // Act
                val actual = createSender().validate()

                // Assert
                assertThat(actual).isEqualTo(StakingTransactionVerdict.WARNING)
                coVerify(exactly = 1) {
                    checkStakingTransactionUseCase(
                        network = NetworkType.ETHEREUM,
                        accountAddress = SOURCE_ADDRESS,
                        unsignedTransaction = EXPECTED_JSON,
                        token = "ETH",
                        blockchain = "Ethereum",
                        provider = "p2p-ethereum-pooled",
                    )
                }
            }

        @Test
        fun `GIVEN odd length hex values WHEN validate THEN hex padded to even length`() = runTest {
            // Arrange
            coEvery {
                transactionCreator.createTransaction(any())
            } returns unsignedTx(value = BigDecimal.ZERO, gasLimit = BigDecimal("15")).right()
            val unsignedSlot = slot<String>()
            coEvery {
                checkStakingTransactionUseCase(network = any(), accountAddress = any(), unsignedTransaction = capture(unsignedSlot), token = any(), blockchain = any(), provider = any())
            } returns StakingTransactionVerdict.SAFE

            // Act
            createSender().validate()

            // Assert
            assertThat(unsignedSlot.captured).contains("\"value\":\"0x00\"")
            assertThat(unsignedSlot.captured).contains("\"gasLimit\":\"0x0f\"")
            assertThat(unsignedSlot.captured).doesNotContain("\"gasPrice\"")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class VerdictCache {

        @Test
        fun `GIVEN validated UNSAFE WHEN send THEN blocked by security validation`() = runTest {
            // Arrange
            givenVerdict(StakingTransactionVerdict.UNSAFE)
            val sender = createSender()
            val recorder = Recorder()

            // Act
            sender.validate()
            sender.send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).containsExactly(BLOCKED_ERROR)
            coVerify(exactly = 0) { prepareForSendUseCase(transactionData = any<TransactionData>(), any(), any()) }
        }

        @Test
        fun `GIVEN not validated and check UNSAFE WHEN send THEN checked in send and blocked`() = runTest {
            // Arrange
            givenVerdict(StakingTransactionVerdict.UNSAFE)
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            coVerify(exactly = 1) { checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                checkStakingTransactionUseCase(
                    network = NetworkType.ETHEREUM,
                    accountAddress = SOURCE_ADDRESS,
                    unsignedTransaction = EXPECTED_JSON,
                    token = "ETH",
                    blockchain = "Ethereum",
                    provider = "p2p-ethereum-pooled",
                )
            }
            assertThat(recorder.constructErrors).containsExactly(BLOCKED_ERROR)
            coVerify(exactly = 0) { prepareForSendUseCase(transactionData = any<TransactionData>(), any(), any()) }
        }

        @Test
        fun `GIVEN not validated and check WARNING WHEN send THEN not blocked and broadcast`() = runTest {
            // Arrange
            givenVerdict(StakingTransactionVerdict.WARNING)
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).isEmpty()
            assertThat(recorder.successUrls).containsExactly("https://etherscan.io/tx/0xhash")
            coVerify(exactly = 1) {
                repository.broadcastTransaction(network = P2PEthPoolNetwork.MAINNET, signedTransaction = "0x0abc")
            }
        }

        private fun givenVerdict(verdict: StakingTransactionVerdict) {
            coEvery {
                checkStakingTransactionUseCase(network = any(), accountAddress = any(), unsignedTransaction = any(), token = any(), blockchain = any(), provider = any())
            } returns verdict
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Send {

        @Test
        fun `GIVEN params missing WHEN send THEN invalid state construct error`() = runTest {
            // Arrange
            every { transactionCreator.extractParams(any()) } returns null
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            assertThat(
                recorder.constructErrors,
            ).containsExactly(StakingError.DomainError("Invalid state for transaction"))
            coVerify(exactly = 0) { transactionCreator.createTransaction(any()) }
        }

        @Test
        fun `GIVEN creator left WHEN send THEN creator error forwarded as construct error`() = runTest {
            // Arrange
            coEvery {
                transactionCreator.createTransaction(any())
            } returns StakingError.DomainError("p2p failed").left()
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).containsExactly(StakingError.DomainError("p2p failed"))
            coVerify(exactly = 0) { prepareForSendUseCase(transactionData = any<TransactionData>(), any(), any()) }
        }

        @Test
        fun `GIVEN safe transaction WHEN send THEN compiled json signed for wallet and network`() = runTest {
            // Act
            createSender().send(Recorder().callbacks)

            // Assert
            val expectedData = TransactionData.Compiled(value = TransactionData.Compiled.Data.RawString(EXPECTED_JSON))
            coVerify {
                prepareForSendUseCase(
                    transactionData = expectedData,
                    userWallet = realUserWallet,
                    network = ethereumCoin.network,
                )
            }
        }

        @Test
        fun `GIVEN prepare for send left WHEN send THEN send error and no broadcast`() = runTest {
            // Arrange
            coEvery {
                prepareForSendUseCase(transactionData = any<TransactionData>(), userWallet = any(), network = any())
            } returns SendTransactionError.UserCancelledError.left()
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.sendErrors).containsExactly(SendTransactionError.UserCancelledError)
            coVerify(exactly = 0) { repository.broadcastTransaction(any(), any()) }
            assertThat(recorder.constructErrors).isEmpty()
        }

        @Test
        fun `GIVEN signed bytes WHEN send THEN broadcast lowercase 0x hex on mainnet`() = runTest {
            // Act
            createSender().send(Recorder().callbacks)

            // Assert
            coVerify(exactly = 1) {
                repository.broadcastTransaction(network = P2PEthPoolNetwork.MAINNET, signedTransaction = "0x0abc")
            }
        }

        // Pins current behaviour: broadcast failure goes to onConstructError, so no TransactionError analytics
        @Test
        fun `GIVEN broadcast left WHEN send THEN construct error not send error`() = runTest {
            // Arrange
            coEvery {
                repository.broadcastTransaction(any(), any())
            } returns StakingError.DomainError("broadcast failed").left()
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).containsExactly(StakingError.DomainError("broadcast failed"))
            assertThat(recorder.sendErrors).isEmpty()
            assertThat(recorder.successUrls).isEmpty()
            verify(exactly = 0) { balanceUpdater.updateAfterTransaction() }
        }

        // Pins current behaviour: a successful P2P send never calls onConstructSuccess, unlike the StakeKit sender
        @Test
        fun `GIVEN broadcast right WHEN send THEN explorer url of hash and balance update without construct success`() =
            runTest {
                // Arrange
                val recorder = Recorder()

                // Act
                createSender().send(recorder.callbacks)

                // Assert
                assertThat(recorder.successUrls).containsExactly("https://etherscan.io/tx/0xhash")
                assertThat(recorder.constructSuccessCalls).isEqualTo(0)
                verify { getExplorerTransactionUrlUseCase(txHash = "0xhash", currency = ethereumCoin) }
                verify(exactly = 1) {
                    balanceUpdaterFactory.create(status(currency = ethereumCoin), realUserWallet, integration)
                }
                verify(exactly = 1) { balanceUpdater.updateAfterTransaction() }
            }

        @Test
        fun `GIVEN explorer url left WHEN send THEN success with empty url`() = runTest {
            // Arrange
            every {
                getExplorerTransactionUrlUseCase(txHash = any(), currency = any())
            } returns TxStatusError.EmptyUrlError.left()
            val recorder = Recorder()

            // Act
            createSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.successUrls).containsExactly("")
        }
    }

    private fun createSender() = P2PEthPoolTransactionSender(
        transactionCreator = transactionCreator,
        stakingBalanceUpdater = balanceUpdaterFactory,
        prepareForSendUseCase = prepareForSendUseCase,
        getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
        p2pEthPoolRepository = repository,
        checkStakingTransactionUseCase = checkStakingTransactionUseCase,
        cryptoCurrencyStatusProvider = Provider { status(currency = ethereumCoin) },
        userWallet = realUserWallet,
        integration = integration,
    )

    private class Recorder {
        val constructErrors = mutableListOf<StakingError>()
        val sendErrors = mutableListOf<SendTransactionError>()
        val successUrls = mutableListOf<String>()
        var constructSuccessCalls = 0

        val callbacks = StakingTransactionSender.Callbacks(
            onConstructSuccess = { constructSuccessCalls++ },
            onConstructError = { constructErrors += it },
            onSendSuccess = { successUrls += it },
            onSendError = { sendErrors += it },
            onFeeIncreased = { _: Fee, _: Boolean -> },
            onTransactionExpired = {},
        )
    }

    private companion object {
        val BLOCKED_ERROR = StakingError.TransactionValidationFailed("Transaction blocked by security validation")

        const val EXPECTED_JSON = "{\"from\":\"0xsource\",\"gasLimit\":\"0x5208\",\"value\":\"0x0de0b6b3a7640000\"," +
            "\"to\":\"0xto\",\"data\":\"0xdeadbeef\",\"nonce\":5,\"type\":2,\"maxFeePerGas\":\"0x06fc23ac00\"," +
            "\"maxPriorityFeePerGas\":\"0x59682f00\",\"chainId\":1}"
    }
}