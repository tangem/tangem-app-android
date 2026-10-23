package com.tangem.data.walletconnect.network.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.domain.walletconnect.usecase.method.WcTransactionTargetCheck.Status
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcEthTransactionTargetValidatorTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val validator = WcEthTransactionTargetValidator(walletManagersFacade)

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade)
    }

    @Test
    fun `GIVEN transaction for another chain WHEN validate THEN blocks signing without checking the recipient`() =
        runTest {
            // Arrange
            val transaction = transaction(chainId = POLYGON_CHAIN_ID_HEX, data = CALL_DATA)

            // Act
            val actual = getEmittedValues(validate(transaction))

            // Assert
            assertThat(actual).containsExactly(Status.ChainMismatch(declaredChainId = POLYGON_CHAIN_ID))
            coVerify(exactly = 0) { walletManagersFacade.hasContractCode(any(), any(), any()) }
        }

    @Test
    fun `GIVEN transaction for the requested chain WHEN validate THEN checks the recipient`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.hasContractCode(any(), any(), any()) } returns true
        val transaction = transaction(chainId = ETHEREUM_CHAIN_ID_HEX, data = CALL_DATA)

        // Act
        val actual = getEmittedValues(validate(transaction))

        // Assert
        assertThat(actual).containsExactly(Status.Checking, Status.Clear).inOrder()
    }

    @Test
    fun `GIVEN no declared chain WHEN validate THEN checks the recipient`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.hasContractCode(any(), any(), any()) } returns true
        val transaction = transaction(chainId = null, data = CALL_DATA)

        // Act
        val actual = getEmittedValues(validate(transaction))

        // Assert
        assertThat(actual).containsExactly(Status.Checking, Status.Clear).inOrder()
    }

    @Test
    fun `GIVEN recipient without code WHEN validate THEN warns without blocking signing`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.hasContractCode(any(), any(), any()) } returns false
        val transaction = transaction(data = CALL_DATA)

        // Act
        val actual = getEmittedValues(validate(transaction))

        // Assert
        assertThat(actual).containsExactly(Status.Checking, Status.RecipientIsNotContract).inOrder()
        assertThat(Status.RecipientIsNotContract.isSigningBlocked).isFalse()
    }

    @Test
    fun `GIVEN recipient check fails WHEN validate THEN leaves signing available`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.hasContractCode(any(), any(), any()) } returns null
        val transaction = transaction(data = CALL_DATA)

        // Act
        val actual = getEmittedValues(validate(transaction))

        // Assert
        assertThat(actual).containsExactly(Status.Checking, Status.Clear).inOrder()
    }

    @Test
    fun `GIVEN transaction without call data WHEN validate THEN skips the recipient check`() = runTest {
        // Arrange
        val transaction = transaction(data = null)

        // Act
        val actual = getEmittedValues(validate(transaction))

        // Assert
        assertThat(actual).containsExactly(Status.Clear)
        coVerify(exactly = 0) { walletManagersFacade.hasContractCode(any(), any(), any()) }
    }

    @Test
    fun `GIVEN bare hex prefix as call data WHEN validate THEN skips the recipient check`() = runTest {
        // Arrange
        val transaction = transaction(data = "0x")

        // Act
        val actual = getEmittedValues(validate(transaction))

        // Assert
        assertThat(actual).containsExactly(Status.Clear)
        coVerify(exactly = 0) { walletManagersFacade.hasContractCode(any(), any(), any()) }
    }

    private fun validate(transaction: WcEthTransactionParams) = validator.validate(
        transaction = transaction,
        userWalletId = USER_WALLET_ID,
        network = ETHEREUM_NETWORK,
    )

    private fun transaction(chainId: String? = null, data: String? = null) = WcEthTransactionParams(
        from = "0xSender",
        to = "0xRecipient",
        data = data,
        gas = null,
        gasPrice = null,
        value = null,
        nonce = null,
        chainId = chainId,
    )

    private companion object {
        const val CALL_DATA = "0xa9059cbb"
        const val ETHEREUM_CHAIN_ID_HEX = "0x1"
        const val POLYGON_CHAIN_ID_HEX = "0x89"
        const val POLYGON_CHAIN_ID = 137
        val USER_WALLET_ID = UserWalletId("011")
        val ETHEREUM_NETWORK: Network = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum).network
    }
}