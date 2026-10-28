package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.WalletBalanceState
import com.tangem.domain.wallets.repository.WalletBalanceStateRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckIsWalletToppedUpUseCaseTest {

    private val repository: WalletBalanceStateRepository = mockk(relaxUnitFun = true)

    private val useCase = CheckIsWalletToppedUpUseCase(walletBalanceStateRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored and current balance states WHEN invoke THEN topped up only on transition`(
        model: InvokeModel,
    ) = runTest {
        // Arrange
        coEvery { repository.getWalletBalanceState(USER_WALLET_ID) } returns model.stored

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, balanceState = model.current)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(model.expected))
        coVerify(exactly = if (model.isStored) 1 else 0) {
            repository.setWalletBalanceState(USER_WALLET_ID, model.current)
        }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val exception = IllegalStateException("corrupt store")
        coEvery { repository.getWalletBalanceState(USER_WALLET_ID) } throws exception

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, balanceState = WalletBalanceState.ToppedUp)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(exception))
    }

    private fun provideTestModels() = listOf(
        InvokeModel(
            stored = WalletBalanceState.ToppedUp,
            current = WalletBalanceState.Empty,
            expected = false,
            isStored = false,
        ),
        InvokeModel(
            stored = null,
            current = WalletBalanceState.ToppedUp,
            expected = false,
            isStored = true,
        ),
        InvokeModel(
            stored = null,
            current = WalletBalanceState.Empty,
            expected = false,
            isStored = true,
        ),
        InvokeModel(
            stored = WalletBalanceState.Empty,
            current = WalletBalanceState.ToppedUp,
            expected = true,
            isStored = true,
        ),
        InvokeModel(
            stored = WalletBalanceState.Error,
            current = WalletBalanceState.ToppedUp,
            expected = true,
            isStored = true,
        ),
        InvokeModel(
            stored = WalletBalanceState.Empty,
            current = WalletBalanceState.Error,
            expected = false,
            isStored = true,
        ),
    )

    data class InvokeModel(
        val stored: WalletBalanceState?,
        val current: WalletBalanceState,
        val expected: Boolean,
        val isStored: Boolean,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
    }
}