package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DerivePolymarketAddressesUseCaseTest {

    private val eoaDeriver: PolymarketEoaDeriver = mockk()
    private val depositWalletDeriver: PolymarketDepositWalletDeriver = mockk()

    private val useCase = DerivePolymarketAddressesUseCase(
        eoaDeriver = eoaDeriver,
        depositWalletDeriver = depositWalletDeriver,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(eoaDeriver, depositWalletDeriver)
    }

    @Test
    fun `GIVEN an owner key WHEN invoke THEN derives the deposit wallet from that owner`() = runTest {
        // Arrange
        coEvery { eoaDeriver.deriveOwnerEoa(userWalletId) } returns OWNER.right()
        every { depositWalletDeriver.deriveDepositWallet(OWNER) } returns DEPOSIT_WALLET

        // Act
        val actual = useCase(userWalletId = userWalletId)

        // Assert
        val expected = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = userWalletId,
        )
        assertThat(actual).isEqualTo(expected.right())
        verify(exactly = 1) { depositWalletDeriver.deriveDepositWallet(OWNER) }
    }

    @Test
    fun `GIVEN the deposit deriver throws WHEN invoke THEN returns Unknown`() = runTest {
        // Arrange
        coEvery { eoaDeriver.deriveOwnerEoa(userWalletId) } returns OWNER.right()
        every { depositWalletDeriver.deriveDepositWallet(OWNER) } throws IllegalArgumentException("boom")

        // Act
        val actual = useCase(userWalletId = userWalletId)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Unknown.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN owner derivation fails WHEN invoke THEN wraps the cause and never derives a wallet`(
        cause: PolymarketDerivationError,
    ) = runTest {
        // Arrange
        coEvery { eoaDeriver.deriveOwnerEoa(userWalletId) } returns cause.left()

        // Act
        val actual = useCase(userWalletId = userWalletId)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Derivation(cause).left())
        verify(exactly = 0) { depositWalletDeriver.deriveDepositWallet(any()) }
    }

    private fun provideTestModels() = listOf(
        PolymarketDerivationError.MissingWallet,
        PolymarketDerivationError.UserCancelled,
        PolymarketDerivationError.DerivationUnsupported,
        PolymarketDerivationError.CardError,
        PolymarketDerivationError.Unknown,
    )

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
    }
}