package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CardIssueOffers
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class GetCustomerOffersUseCaseTest {

    private val customerOffersRepository: CustomerOffersRepository = mockk()

    private val useCase = GetCustomerOffersUseCase(customerOffersRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(customerOffersRepository)
    }

    @Test
    fun `GIVEN both offers WHEN cardIssueOffers THEN each is selected by its type`() = runTest {
        // Arrange
        givenOffers(plasticOffer(), virtualOffer())

        // Act
        val actual = useCase.cardIssueOffers(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CardIssueOffers(virtual = virtualOffer(), plastic = plasticOffer()).right())
        coVerify(exactly = 1) { customerOffersRepository.getOffers(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN only the plastic offer WHEN cardIssueOffers THEN the virtual one is null`() = runTest {
        // Arrange
        givenOffers(plasticOffer())

        // Act
        val actual = useCase.cardIssueOffers(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CardIssueOffers(virtual = null, plastic = plasticOffer()).right())
    }

    @Test
    fun `GIVEN only the virtual offer WHEN cardIssueOffers THEN the plastic one is null`() = runTest {
        // Arrange
        givenOffers(virtualOffer())

        // Act
        val actual = useCase.cardIssueOffers(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CardIssueOffers(virtual = virtualOffer(), plastic = null).right())
    }

    @Test
    fun `GIVEN no offers WHEN cardIssueOffers THEN an empty result is returned instead of an error`() = runTest {
        // Arrange
        givenOffers()

        // Act
        val actual = useCase.cardIssueOffers(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CardIssueOffers(virtual = null, plastic = null).right())
        assertThat(actual.getOrNull()?.hasAny).isFalse()
    }

    @Test
    fun `GIVEN an unknown offer type WHEN cardIssueOffers THEN it matches neither slot`() = runTest {
        // Arrange
        givenOffers(offer(type = Offer.Type.UNKNOWN))

        // Act
        val actual = useCase.cardIssueOffers(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CardIssueOffers(virtual = null, plastic = null).right())
    }

    @Test
    fun `GIVEN the request fails WHEN cardIssueOffers THEN the error is propagated`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(USER_WALLET_ID) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val actual = useCase.cardIssueOffers(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
    }

    @Test
    fun `GIVEN both offers WHEN additionalCardOffer THEN it returns the virtual slot`() = runTest {
        // Arrange
        givenOffers(plasticOffer(), virtualOffer())

        // Act
        val actual = useCase.additionalCardOffer(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(virtualOffer().right())
        coVerify(exactly = 1) { customerOffersRepository.getOffers(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN only the plastic offer WHEN additionalCardOffer THEN it returns null`() = runTest {
        // Arrange
        givenOffers(plasticOffer())

        // Act
        val actual = useCase.additionalCardOffer(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    private fun givenOffers(vararg offers: Offer) {
        coEvery { customerOffersRepository.getOffers(USER_WALLET_ID) } returns offers.toList().right()
    }

    private fun plasticOffer() = offer(type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN)

    private fun virtualOffer() = offer(type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN)

    private fun offer(type: Offer.Type) = Offer(
        type = type,
        fee = Offer.Fee(amount = BigDecimal("5.00"), currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = "spec", orderType = OrderType.UNKNOWN),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("1234567890ABCDEF")
    }
}