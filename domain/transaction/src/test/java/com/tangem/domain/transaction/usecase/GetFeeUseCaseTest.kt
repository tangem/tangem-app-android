package com.tangem.domain.transaction.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for [GetFeeUseCase] — the compiled-transaction overload that decides between the new
 * simulated `estimateFee` path and the legacy `getFee` path.
 *
 * The simulated estimation is selected only when ALL of these hold:
 *  - [GetFeeUseCase.invoke] is called with `isSimulateEstimation = true`
 *  - `spenderAddress != null`
 *  - the resolved transaction sender is an [EthereumWalletManager]
 *
 * Any other combination falls back to the legacy `getFee(transactionData)`.
 */
internal class GetFeeUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val demoConfig: DemoConfig = mockk()

    private val useCase = GetFeeUseCase(
        walletManagersFacade = walletManagersFacade,
        demoConfig = demoConfig,
    )

    private val network: Network = mockk(relaxed = true)
    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val userWallet: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns userWalletId
    }
    private val transactionData: TransactionData = mockk(relaxed = true)
    private val expectedFee: TransactionFee = mockk(relaxed = true)

    private val ethereumWalletManager: EthereumWalletManager = mockk()
    private val plainWalletManager: WalletManager = mockk()

    @Before
    fun setUp() {
        every { demoConfig.isDemoCardId(any()) } returns false
    }

    @Test
    fun `GIVEN simulate + spender + ethereum manager THEN estimateFee is used`() = runTest {
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns ethereumWalletManager
        coEvery {
            ethereumWalletManager.estimateFeeWithOverride(
                transactionData = transactionData,
                spenderAddress = SPENDER,
                isSimulate = true,
            )
        } returns Result.Success(expectedFee)

        val result = useCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            spenderAddress = SPENDER,
            isSimulateEstimation = true,
        )

        assertThat(result).isEqualTo(expectedFee.right())
        coVerify(exactly = 1) {
            ethereumWalletManager.estimateFeeWithOverride(
                transactionData = transactionData,
                spenderAddress = SPENDER,
                isSimulate = true,
            )
        }
        coVerify(exactly = 0) { ethereumWalletManager.getFee(transactionData = transactionData) }
    }

    @Test
    fun `GIVEN simulate false THEN legacy getFee is used even for ethereum manager`() = runTest {
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns ethereumWalletManager
        coEvery { ethereumWalletManager.getFee(transactionData = transactionData) } returns
            Result.Success(expectedFee)

        val result = useCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            spenderAddress = SPENDER,
            isSimulateEstimation = false,
        )

        assertThat(result).isEqualTo(expectedFee.right())
        coVerify(exactly = 1) { ethereumWalletManager.getFee(transactionData = transactionData) }
        coVerify(exactly = 0) {
            ethereumWalletManager.estimateFeeWithOverride(
                transactionData = any(),
                spenderAddress = any(),
                isSimulate = any(),
            )
        }
    }

    @Test
    fun `GIVEN null spender THEN legacy getFee is used even when simulate true`() = runTest {
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns ethereumWalletManager
        coEvery { ethereumWalletManager.getFee(transactionData = transactionData) } returns
            Result.Success(expectedFee)

        val result = useCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            spenderAddress = null,
            isSimulateEstimation = true,
        )

        assertThat(result).isEqualTo(expectedFee.right())
        coVerify(exactly = 1) { ethereumWalletManager.getFee(transactionData = transactionData) }
        coVerify(exactly = 0) {
            ethereumWalletManager.estimateFeeWithOverride(
                transactionData = any(),
                spenderAddress = any(),
                isSimulate = any(),
            )
        }
    }

    @Test
    fun `GIVEN non-ethereum manager THEN legacy getFee is used even when simulate plus spender`() = runTest {
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns plainWalletManager
        coEvery { plainWalletManager.getFee(transactionData = transactionData) } returns
            Result.Success(expectedFee)

        val result = useCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            spenderAddress = SPENDER,
            isSimulateEstimation = true,
        )

        assertThat(result).isEqualTo(expectedFee.right())
        coVerify(exactly = 1) { plainWalletManager.getFee(transactionData = transactionData) }
    }

    @Test
    fun `GIVEN getFee returns failure THEN error is mapped to GetFeeError`() = runTest {
        val failure = Result.Failure(com.tangem.blockchain.common.BlockchainSdkError.CustomError("boom"))
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns plainWalletManager
        coEvery { plainWalletManager.getFee(transactionData = transactionData) } returns failure

        val result = useCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
    }

    @Test
    fun `GIVEN wallet manager is null THEN DataError is raised`() = runTest {
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        } returns null

        val result = useCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            spenderAddress = null,
            isSimulateEstimation = false,
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
    }

    private companion object {
        const val SPENDER = "0xSpender"
    }
}