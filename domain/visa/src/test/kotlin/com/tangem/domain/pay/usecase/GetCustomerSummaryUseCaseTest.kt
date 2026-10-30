package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.repository.CustomerSummaryRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCustomerSummaryUseCaseTest {

    private val customerSummaryRepository: CustomerSummaryRepository = mockk()
    private val cached: CustomerSummary = mockk()
    private val fetched: CustomerSummary = mockk()

    private val useCase = GetCustomerSummaryUseCase(customerSummaryRepository = customerSummaryRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(customerSummaryRepository)
        coEvery { customerSummaryRepository.fetch(USER_WALLET_ID) } returns fetched.right()
    }

    @Test
    fun `GIVEN cached summary WHEN only cache THEN cached returned without request`() = runTest {
        // Arrange
        every { customerSummaryRepository.getFromCache(USER_WALLET_ID) } returns cached

        // Act
        val actual = useCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE)

        // Assert
        assertThat(actual).isEqualTo(cached.right())
        coVerify(exactly = 0) { customerSummaryRepository.fetch(any()) }
    }

    @Test
    fun `GIVEN empty cache WHEN only cache THEN null returned without request`() = runTest {
        // Arrange
        every { customerSummaryRepository.getFromCache(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE)

        // Assert
        assertThat(actual).isEqualTo(null.right())
        coVerify(exactly = 0) { customerSummaryRepository.fetch(any()) }
    }

    @Test
    fun `GIVEN cached summary WHEN cache first THEN cached returned without request`() = runTest {
        // Arrange
        every { customerSummaryRepository.getFromCache(USER_WALLET_ID) } returns cached

        // Act
        val actual = useCase(USER_WALLET_ID, CachePolicy.CACHE_FIRST)

        // Assert
        assertThat(actual).isEqualTo(cached.right())
        coVerify(exactly = 0) { customerSummaryRepository.fetch(any()) }
    }

    @Test
    fun `GIVEN empty cache WHEN cache first THEN summary fetched`() = runTest {
        // Arrange
        every { customerSummaryRepository.getFromCache(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(USER_WALLET_ID, CachePolicy.CACHE_FIRST)

        // Assert
        assertThat(actual).isEqualTo(fetched.right())
    }

    @Test
    fun `GIVEN cached summary WHEN refresh THEN summary fetched`() = runTest {
        // Arrange
        every { customerSummaryRepository.getFromCache(USER_WALLET_ID) } returns cached

        // Act
        val actual = useCase(USER_WALLET_ID, CachePolicy.REFRESH)

        // Assert
        assertThat(actual).isEqualTo(fetched.right())
    }

    @Test
    fun `GIVEN failing request WHEN refresh THEN error returned`() = runTest {
        // Arrange
        coEvery { customerSummaryRepository.fetch(USER_WALLET_ID) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(USER_WALLET_ID, CachePolicy.REFRESH)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}