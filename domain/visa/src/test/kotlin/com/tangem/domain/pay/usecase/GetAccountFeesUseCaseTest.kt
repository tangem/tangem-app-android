package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetAccountFeesUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()

    private val useCase = GetAccountFeesUseCase(onboardingRepository = onboardingRepository)

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository)
    }

    @Test
    fun `GIVEN fee types WHEN invoke THEN repository requested with them`() = runTest {
        // Arrange
        val types = setOf(AccountFeeGroupType.ONRAMP, AccountFeeGroupType.OFFRAMP)
        val fees = listOf(fee(group = AccountFeeGroupType.ONRAMP), fee(group = AccountFeeGroupType.OFFRAMP))
        coEvery { onboardingRepository.getFees(userWalletId, types) } returns fees.right()

        // Act
        val actual = useCase(userWalletId = userWalletId, types = types)

        // Assert
        assertThat(actual).isEqualTo(fees.right())
        coVerify(exactly = 1) { onboardingRepository.getFees(userWalletId, types) }
    }

    @Test
    fun `GIVEN fees unavailable WHEN invoke THEN error`() = runTest {
        // Arrange
        val types = setOf(AccountFeeGroupType.ONRAMP)
        coEvery { onboardingRepository.getFees(userWalletId, types) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val actual = useCase(userWalletId = userWalletId, types = types)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
    }

    private fun fee(group: AccountFeeGroupType) = AccountFee(
        type = "ACH_$group",
        name = "ACH",
        paymentMethodId = "ACH",
        group = group,
        amount = BigDecimal.ONE,
        currency = "USD",
    )
}