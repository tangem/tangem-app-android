package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.model.TangemPayCampaignResolution
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class ResolveTangemPayCampaignUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()
    private val campaignRepository: TangemPayCampaignRepository = mockk()
    private val eligibilityManager: TangemPayEligibilityManager = mockk()
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase = mockk()

    private val useCase = ResolveTangemPayCampaignUseCase(
        onboardingRepository = onboardingRepository,
        campaignRepository = campaignRepository,
        eligibilityManager = eligibilityManager,
        getCustomerOffersUseCase = getCustomerOffersUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository, campaignRepository, eligibilityManager, getCustomerOffersUseCase)
        coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns true.right()
        coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns CAMPAIGN.right()
        coEvery { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false) } returns emptyList()
    }

    @Nested
    inner class Campaign {

        @Test
        fun `GIVEN a valid campaign WHEN invoked THEN the link is validated AND landing is resolved`() = runTest {
            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
            coVerify(exactly = 1) { onboardingRepository.validateDeeplink("tangem://marketing/$CAMPAIGN_KEY") }
        }

        @Test
        fun `GIVEN a campaign without art WHEN invoked THEN landing is resolved with no art`() = runTest {
            // Arrange
            coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns TangemPayCampaign(cardArtUrl = null).right()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(
                TangemPayCampaignResolution.Landing(TangemPayCampaign(cardArtUrl = null)),
            )
        }

        @Test
        fun `GIVEN an invalid campaign WHEN invoked THEN unavailable is resolved AND campaign is not loaded`() =
            runTest {
                // Arrange
                coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns false.right()

                // Act
                val result = useCase(CAMPAIGN_KEY)

                // Assert
                assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Unavailable)
                coVerify(exactly = 0) { campaignRepository.getCampaign(any()) }
            }

        @Test
        fun `GIVEN validation fails WHEN invoked THEN the error is propagated AND campaign is not loaded`() = runTest {
            // Arrange
            coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns
                VisaApiError.ServerUnavailable.left()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
            coVerify(exactly = 0) { campaignRepository.getCampaign(any()) }
        }

        @Test
        fun `GIVEN the campaign request fails WHEN invoked THEN the error is propagated`() = runTest {
            // Arrange
            coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns VisaApiError.Unspecified.left()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        }
    }

    @Nested
    inner class IssueCardFastPath {

        @Test
        fun `GIVEN a single approved wallet with a live session WHEN invoked THEN issue card is resolved`() = runTest {
            // Arrange
            givenIssuableWallet()
            coEvery { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) } returns OFFER.right()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(
                TangemPayCampaignResolution.IssueCard(
                    userWalletId = WALLET_ID,
                    campaign = CAMPAIGN,
                    offer = OFFER,
                ),
            )
        }

        @Test
        fun `GIVEN several compatible wallets WHEN invoked THEN landing is resolved AND no customer info requested`() =
            runTest {
                // Arrange
                givenIssuableWallet()
                coEvery {
                    eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false)
                } returns listOf(WALLET_ID, OTHER_WALLET_ID)

                // Act
                val result = useCase(CAMPAIGN_KEY)

                // Assert
                assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
                coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
            }

        @Test
        fun `GIVEN the single wallet has no Pay WHEN invoked THEN landing is resolved AND no customer info`() =
            runTest {
                // Arrange
                givenIssuableWallet()
                coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns false.right()

                // Act
                val result = useCase(CAMPAIGN_KEY)

                // Assert
                assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
                coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
            }

        @Test
        fun `GIVEN the Pay check fails WHEN invoked THEN landing is resolved`() = runTest {
            // Arrange
            givenIssuableWallet()
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns VisaApiError.Unspecified.left()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
        }

        @Test
        fun `GIVEN missing Pay credentials WHEN invoked THEN landing is resolved AND no customer info requested`() =
            runTest {
                // Arrange
                givenIssuableWallet()
                coEvery { onboardingRepository.isTangemPayInitialDataProduced(WALLET_ID) } returns false

                // Act
                val result = useCase(CAMPAIGN_KEY)

                // Assert
                assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
                coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
            }

        @Test
        fun `GIVEN an expired Pay session WHEN invoked THEN landing is resolved AND no offer requested`() = runTest {
            // Arrange
            givenIssuableWallet()
            coEvery { onboardingRepository.getCustomerInfo(WALLET_ID) } returns VisaApiError.RefreshTokenExpired.left()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
            coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
        }

        @Test
        fun `GIVEN KYC is not approved WHEN invoked THEN landing is resolved AND no offer requested`() = runTest {
            // Arrange
            givenIssuableWallet()
            givenKycStatus(KycStatus.PENDING)

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
            coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
        }

        @Test
        fun `GIVEN no campaign offer WHEN invoked THEN landing is resolved`() = runTest {
            // Arrange
            givenIssuableWallet()
            coEvery { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) } returns null.right()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
        }

        @Test
        fun `GIVEN the offers request fails WHEN invoked THEN landing is resolved`() = runTest {
            // Arrange
            givenIssuableWallet()
            coEvery {
                getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY)
            } returns VisaApiError.ServerUnavailable.left()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
        }
    }

    private fun givenIssuableWallet() {
        coEvery { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false) } returns
            listOf(WALLET_ID)
        coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns true.right()
        coEvery { onboardingRepository.isTangemPayInitialDataProduced(WALLET_ID) } returns true
        givenKycStatus(KycStatus.APPROVED)
        coEvery { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) } returns OFFER.right()
    }

    private fun givenKycStatus(status: KycStatus) {
        val customerInfo = mockk<CustomerInfo>(relaxed = true)
        every { customerInfo.kycStatus } returns status
        coEvery { onboardingRepository.getCustomerInfo(WALLET_ID) } returns customerInfo.right()
    }

    private companion object {
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val CAMPAIGN_LINK = "tangem://marketing/tpay_for_oxford"
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("012")
        val CAMPAIGN = TangemPayCampaign(cardArtUrl = "https://example.com/oxford.png")
        val OFFER = Offer(
            type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK,
            fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
            data = Offer.Data(specificationName = "SP_000009", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2),
        )
    }
}