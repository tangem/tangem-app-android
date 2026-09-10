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
        coEvery { eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false) } returns emptyList()
    }

    @Test
    fun `GIVEN no wallets WHEN invoked THEN the campaign link is validated AND landing is resolved`() = runTest {
        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
        coVerify(exactly = 1) { onboardingRepository.validateDeeplink("tangem://marketing/$CAMPAIGN_KEY") }
        coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
    }

    @Test
    fun `GIVEN wallets without Pay WHEN invoked THEN landing is resolved`() = runTest {
        // Arrange
        givenWallets(WALLET_ID to false, OTHER_WALLET_ID to false)

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
    }

    @Test
    fun `GIVEN a wallet with Pay WHEN invoked THEN the offer is requested for it AND issue card is resolved`() =
        runTest {
            // Arrange
            givenWallets(WALLET_ID to false, OTHER_WALLET_ID to true)
            coEvery { getCustomerOffersUseCase.campaignOffer(OTHER_WALLET_ID, CAMPAIGN_KEY) } returns OFFER.right()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(
                TangemPayCampaignResolution.IssueCard(
                    userWalletId = OTHER_WALLET_ID,
                    campaign = CAMPAIGN,
                    offer = OFFER,
                ),
            )
        }

    @Test
    fun `GIVEN a wallet with Pay and no campaign offer WHEN invoked THEN unavailable AND art is not requested`() =
        runTest {
            // Arrange
            givenWallets(WALLET_ID to true)
            coEvery { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) } returns null.right()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Unavailable)
            coVerify(exactly = 0) { campaignRepository.getCampaign(any()) }
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
    fun `GIVEN an invalid campaign WHEN invoked THEN unavailable is resolved AND campaign is not loaded`() = runTest {
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
        coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns VisaApiError.ServerUnavailable.left()

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

    @Test
    fun `GIVEN the Pay check fails WHEN invoked THEN the error is propagated`() = runTest {
        // Arrange
        coEvery { eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false) } returns
            listOf(WALLET_ID)
        coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns VisaApiError.Unspecified.left()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    @Test
    fun `GIVEN the offers request fails WHEN invoked THEN the error is propagated`() = runTest {
        // Arrange
        givenWallets(WALLET_ID to true)
        coEvery { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) } returns
            VisaApiError.ServerUnavailable.left()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
    }

    @Test
    fun `GIVEN a Pay wallet with pending KYC WHEN invoked THEN landing is resolved AND no offer requested`() =
        runTest {
            // Arrange
            givenWallets(WALLET_ID to true)
            givenKycStatus(WALLET_ID, KycStatus.PENDING)

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
            coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
        }

    @Test
    fun `GIVEN a Pay wallet with rejected KYC WHEN invoked THEN landing is resolved`() = runTest {
        // Arrange
        givenWallets(WALLET_ID to true)
        givenKycStatus(WALLET_ID, KycStatus.REJECTED)

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
    }

    @Test
    fun `GIVEN a pending Pay wallet before an approved one WHEN invoked THEN the approved one issues the card`() =
        runTest {
            // Arrange
            givenWallets(WALLET_ID to true, OTHER_WALLET_ID to true)
            givenKycStatus(WALLET_ID, KycStatus.PENDING)
            coEvery { getCustomerOffersUseCase.campaignOffer(OTHER_WALLET_ID, CAMPAIGN_KEY) } returns OFFER.right()

            // Act
            val result = useCase(CAMPAIGN_KEY)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(
                TangemPayCampaignResolution.IssueCard(
                    userWalletId = OTHER_WALLET_ID,
                    campaign = CAMPAIGN,
                    offer = OFFER,
                ),
            )
        }

    @Test
    fun `GIVEN the customer info request fails WHEN invoked THEN the error is propagated`() = runTest {
        // Arrange
        givenWallets(WALLET_ID to true)
        coEvery { onboardingRepository.getCustomerInfo(WALLET_ID) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
        coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
    }

    private fun givenWallets(vararg wallets: Pair<UserWalletId, Boolean>) {
        coEvery { eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false) } returns
            wallets.map { it.first }
        wallets.forEach { (walletId, hasPay) ->
            coEvery { onboardingRepository.hasTangemPayInWallet(walletId) } returns hasPay.right()
            if (hasPay) givenKycStatus(walletId, KycStatus.APPROVED)
        }
    }

    private fun givenKycStatus(walletId: UserWalletId, status: KycStatus) {
        val customerInfo = mockk<CustomerInfo>(relaxed = true)
        every { customerInfo.kycStatus } returns status
        coEvery { onboardingRepository.getCustomerInfo(walletId) } returns customerInfo.right()
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