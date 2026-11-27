package com.tangem.domain.earn.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.earn.MockEarn
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTopEarnTokensUseCaseTest {

    private val repository: EarnRepository = mockk()

    private val useCase = GetTopEarnTokensUseCase(repository)

    private val staking = earnToken(type = EarnType.STAKING)
    private val vault = earnToken(type = EarnType.VAULT)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: VaultsModel) = runTest {
        // Arrange
        val stored: EarnTopToken = listOf(staking, vault).right()
        every { repository.observeTopEarnTokens() } returns flowOf(stored)

        // Act
        val actual = useCase(isVaultsIncluded = model.isVaultsIncluded).first()

        // Assert
        assertThat(actual).isEqualTo(model.expected.right())
    }

    private fun provideTestModels() = listOf(
        VaultsModel(isVaultsIncluded = true, expected = listOf(staking, vault)),
        VaultsModel(isVaultsIncluded = false, expected = listOf(staking)),
    )

    private fun earnToken(type: EarnType) = EarnTokenWithCurrency(
        networkName = "Ethereum",
        earnToken = MockEarn.yieldToken().copy(type = type),
        cryptoCurrency = mockk(),
    )

    internal data class VaultsModel(val isVaultsIncluded: Boolean, val expected: List<EarnTokenWithCurrency>) {
        override fun toString(): String = if (isVaultsIncluded) {
            "GIVEN vaults included WHEN invoked THEN vaults are kept"
        } else {
            "GIVEN vaults left out WHEN invoked THEN vaults are dropped"
        }
    }
}