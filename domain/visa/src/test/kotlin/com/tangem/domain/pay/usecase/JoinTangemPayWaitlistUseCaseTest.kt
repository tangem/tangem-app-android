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
        coEvery { waitlistRepository.join(any(), any(), any(), any()) } returns Unit.right()
        coEvery { onboardingRepository.disableTangemPay(any()) } returns Unit.right()
        coEvery { paymentAccountStatusFetcher(any<UserWalletId>()) } returns Unit.right()
    }

    @Test
    fun `GIVEN several products WHEN invoke THEN joins each product with entry`() = runTest {
        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            products = PRODUCTS,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        PRODUCTS.forEach { product ->
            coVerify(exactly = 1) { waitlistRepository.join(USER_WALLET_ID, product, ENTRY, any()) }
        }
    }

    @Test
    fun `GIVEN several products WHEN invoke twice THEN keys are unique per product and stable between calls`() =
        runTest {
            // Arrange
            val keys = mutableListOf<Pair<TangemPayProduct, String>>()
            coEvery { waitlistRepository.join(any(), any(), any(), any()) } answers {
                keys += secondArg<TangemPayProduct>() to arg<String>(3)
                Unit.right()
            }

            // Act
            repeat(2) {
                useCase(
                    userWalletId = USER_WALLET_ID,
                    products = PRODUCTS,
                    entry = ENTRY,
                    idempotencyKey = IDEMPOTENCY_KEY,
                )
            }

            // Assert
            val keysByProduct = keys.groupBy(keySelector = { it.first }, valueTransform = { it.second })
            assertThat(keysByProduct.values.map { it.toSet().size }).containsExactly(1, 1)
            assertThat(keysByProduct.values.map { it.first() }.toSet()).hasSize(PRODUCTS.size)
        }

    @Test
    fun `GIVEN one product join fails WHEN invoke THEN returns Left and tangem pay not disabled`() = runTest {
        // Arrange
        coEvery {
            waitlistRepository.join(USER_WALLET_ID, TangemPayProduct.PAYMENT_ACCOUNT, ENTRY, any())
        } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            products = PRODUCTS,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
        coVerify(exactly = 0) { onboardingRepository.disableTangemPay(any()) }
    }

    @Test
    fun `GIVEN no user wallet WHEN invoke THEN passes null userWalletId and tangem pay not disabled`() = runTest {
        // Act
        val actual = useCase(userWalletId = null, products = PRODUCTS, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = PRODUCTS.size) { waitlistRepository.join(null, any(), ENTRY, any()) }
        coVerify(exactly = 0) { onboardingRepository.disableTangemPay(any()) }
    }

    @Test
    fun `GIVEN user wallet WHEN all joins succeed THEN tangem pay disabled once and status refetched`() = runTest {
        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            products = PRODUCTS,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { onboardingRepository.disableTangemPay(USER_WALLET_ID) }
        coVerifyOrder {
            onboardingRepository.disableTangemPay(USER_WALLET_ID)
            paymentAccountStatusFetcher(USER_WALLET_ID)
        }
    }

    @Test
    fun `GIVEN disabling tangem pay fails WHEN join succeeds THEN returns Right without refetch`() = runTest {
        // Arrange
        coEvery { onboardingRepository.disableTangemPay(USER_WALLET_ID) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            products = PRODUCTS,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { paymentAccountStatusFetcher(any<UserWalletId>()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val IDEMPOTENCY_KEY = "idempotency-key-1"
        val PRODUCTS = TangemPayProduct.entries

        val ENTRY = TangemPayWaitlistEntry(
            email = "user@tangem.com",
            countryCode = "US",
            regionCode = "CA",
            isMarketingConsentGiven = true,
        )
    }
}