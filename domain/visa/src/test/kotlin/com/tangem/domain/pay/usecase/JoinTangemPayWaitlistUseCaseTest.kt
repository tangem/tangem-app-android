package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayWaitlistRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JoinTangemPayWaitlistUseCaseTest {

    private val waitlistRepository: TangemPayWaitlistRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = JoinTangemPayWaitlistUseCase(
        waitlistRepository = waitlistRepository,
        onboardingRepository = onboardingRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(waitlistRepository, onboardingRepository, paymentAccountStatusFetcher)
        coEvery { onboardingRepository.disableTangemPay(any()) } returns Unit.right()
        coEvery { paymentAccountStatusFetcher(any<UserWalletId>()) } returns Unit.right()
    }

    @Test
    fun `GIVEN repository succeeds WHEN invoke THEN returns Right and passes arguments`() = runTest {
        // GIVEN
        coEvery { waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY) } returns Unit.right()

        // WHEN
        val actual = useCase(userWalletId = USER_WALLET_ID, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // THEN
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY) }
    }

    @Test
    fun `GIVEN repository fails WHEN invoke THEN returns Left with the same error`() = runTest {
        // GIVEN
        coEvery {
            waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY)
        } returns VisaApiError.Unspecified.left()

        // WHEN
        val actual = useCase(userWalletId = USER_WALLET_ID, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // THEN
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
        coVerify(exactly = 1) { waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY) }
    }

    @Test
    fun `GIVEN no user wallet WHEN invoke THEN passes null userWalletId and tangem pay not disabled`() = runTest {
        // GIVEN
        coEvery { waitlistRepository.join(null, ENTRY, IDEMPOTENCY_KEY) } returns Unit.right()

        // WHEN
        val actual = useCase(userWalletId = null, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // THEN
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { waitlistRepository.join(null, ENTRY, IDEMPOTENCY_KEY) }
        coVerify(exactly = 0) { onboardingRepository.disableTangemPay(any()) }
    }

    @Test
    fun `GIVEN entry without region WHEN invoke THEN passes entry unchanged`() = runTest {
        // GIVEN
        val entry = ENTRY.copy(
            product = TangemPayProduct.PAYMENT_ACCOUNT,
            regionCode = null,
            isMarketingConsentGiven = false,
        )
        coEvery { waitlistRepository.join(USER_WALLET_ID, entry, IDEMPOTENCY_KEY) } returns Unit.right()

        // WHEN
        val actual = useCase(userWalletId = USER_WALLET_ID, entry = entry, idempotencyKey = IDEMPOTENCY_KEY)

        // THEN
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { waitlistRepository.join(USER_WALLET_ID, entry, IDEMPOTENCY_KEY) }
    }

    @Test
    fun `GIVEN user wallet WHEN join succeeds THEN tangem pay disabled and status refetched`() = runTest {
        // Arrange
        coEvery { waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY) } returns Unit.right()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY)
            onboardingRepository.disableTangemPay(USER_WALLET_ID)
            paymentAccountStatusFetcher(USER_WALLET_ID)
        }
    }

    @Test
    fun `GIVEN disabling tangem pay fails WHEN join succeeds THEN returns Right without refetch`() = runTest {
        // Arrange
        coEvery { waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY) } returns Unit.right()
        coEvery { onboardingRepository.disableTangemPay(USER_WALLET_ID) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { paymentAccountStatusFetcher(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN user wallet WHEN join fails THEN tangem pay not disabled`() = runTest {
        // Arrange
        coEvery {
            waitlistRepository.join(USER_WALLET_ID, ENTRY, IDEMPOTENCY_KEY)
        } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
        coVerify(exactly = 0) { onboardingRepository.disableTangemPay(any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val IDEMPOTENCY_KEY = "idempotency-key-1"

        val ENTRY = TangemPayWaitlistEntry(
            email = "user@tangem.com",
            product = TangemPayProduct.BANKING_ACCOUNT,
            countryCode = "US",
            regionCode = "CA",
            isMarketingConsentGiven = true,
        )
    }
}