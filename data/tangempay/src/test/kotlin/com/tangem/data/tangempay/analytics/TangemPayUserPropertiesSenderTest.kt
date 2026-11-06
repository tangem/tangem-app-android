package com.tangem.data.tangempay.analytics

import com.tangem.core.analytics.api.AnalyticsUserPropertiesHandler
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerSummary
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayUserPropertiesSenderTest {

    private val userPropertiesHandler: AnalyticsUserPropertiesHandler = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val selectedWallet = MutableStateFlow<UserWallet?>(null)

    private val sender = TangemPayUserPropertiesSender(
        userPropertiesHandler = userPropertiesHandler,
        userWalletsListRepository = userWalletsListRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userPropertiesHandler, userWalletsListRepository)
        every { userWalletsListRepository.selectedUserWallet } returns selectedWallet
        selectedWallet.value = mockk { every { walletId } returns SELECTED_WALLET_ID }
    }

    @Test
    fun `GIVEN summary of the selected wallet WHEN fetched THEN its user properties set`() {
        // Act
        sender.onSummaryFetched(userWalletId = SELECTED_WALLET_ID, summary = SUMMARY)

        // Assert
        verify(exactly = 1) {
            userPropertiesHandler.setUserProperties(
                mapOf(
                    "pay_kyc_status" to "approved",
                    "pay_card_status" to "active",
                    "pay_account_status" to "issuing",
                ),
            )
        }
    }

    @Test
    fun `GIVEN customer info of the selected wallet WHEN fetched THEN plan property set`() {
        // Arrange
        val customerInfo: CustomerInfo = mockk { every { tariffPlan } returns null }

        // Act
        sender.onCustomerInfoFetched(userWalletId = SELECTED_WALLET_ID, customerInfo = customerInfo)

        // Assert
        verify(exactly = 1) { userPropertiesHandler.setUserProperties(mapOf("pay_plan" to "none")) }
    }

    @Test
    fun `GIVEN summary of another wallet WHEN fetched THEN user properties untouched`() {
        // Act
        sender.onSummaryFetched(userWalletId = UserWalletId("022"), summary = SUMMARY)

        // Assert
        verify(exactly = 0) { userPropertiesHandler.setUserProperties(any()) }
    }

    @Test
    fun `GIVEN no selected wallet WHEN summary fetched THEN user properties untouched`() {
        // Arrange
        selectedWallet.value = null

        // Act
        sender.onSummaryFetched(userWalletId = SELECTED_WALLET_ID, summary = SUMMARY)

        // Assert
        verify(exactly = 0) { userPropertiesHandler.setUserProperties(any()) }
    }

    private companion object {
        val SELECTED_WALLET_ID = UserWalletId("011")

        val SUMMARY = CustomerSummary(
            customerId = "customer",
            customerStatus = CustomerSummary.CustomerStatus.ACTIVE,
            isHidden = false,
            kycStatus = CustomerSummary.KycStatus.APPROVED,
            paymentAccount = CustomerSummary.PaymentAccount(
                id = "payment-account",
                activation = CustomerSummary.PaymentAccount.Activation(
                    status = CustomerSummary.PaymentAccount.Activation.Status.COMPLETED,
                    step = null,
                    failureReason = null,
                ),
                cards = CustomerSummary.Cards(active = 1, frozen = 0, pending = 0),
            ),
            moneyAccount = CustomerSummary.MoneyAccount(
                id = null,
                activation = CustomerSummary.MoneyAccount.Activation(
                    status = CustomerSummary.MoneyAccount.Activation.Status.PROCESSING,
                    step = CustomerSummary.MoneyAccount.Activation.Step.ISSUING,
                    failureReason = null,
                ),
            ),
        )
    }
}