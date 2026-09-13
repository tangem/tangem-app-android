package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetEthSpecificFeeUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk {
        coEvery { getOrCreateWalletManager(any(), any()) } returns mockk<WalletManager>()
    }
    private val userWallet: UserWallet.Hot = mockk(relaxed = true) {
        every { walletId } returns UserWalletId(stringValue = "deadbeef")
    }
    private val useCase = GetEthSpecificFeeUseCase(walletManagersFacade)

    private val gasLimit = BigInteger.valueOf(21_000)
    private val gasPrice = BigInteger.valueOf(20_000_000_000)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN evm coin WHEN invoke THEN fee is converted from wei with 18 decimals`(blockchain: Blockchain) =
        runTest {
            // Arrange
            val coin = MockCryptoCurrencyFactory().createCoin(blockchain)

            // Act
            val actual = useCase(userWallet, coin, gasLimit, gasPrice).getOrNull()!!

            // Assert
            assertThat(actual.minimum.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00042"))
            assertThat(actual.minimum.amount.decimals).isEqualTo(18)
            assertThat(actual.normal.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00063"))
            assertThat(actual.priority.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00084"))
        }

    private fun provideTestModels() = listOf(Blockchain.Ethereum, Blockchain.Arc)
}