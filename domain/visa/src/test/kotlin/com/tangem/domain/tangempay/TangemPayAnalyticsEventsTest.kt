package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayAnalyticsEventsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN visa event WHEN created THEN id and params match the spec`(model: EventModel) {
        // Act
        val event = model.event

        // Assert
        assertThat(event.id).isEqualTo(model.expectedId)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    @ParameterizedTest
    @MethodSource("provideProductSetModels")
    fun `GIVEN offered products WHEN product set picked THEN its value matches the spec`(model: ProductSetModel) {
        // Act
        val productSet = TangemPayAnalyticsEvents.ProductSet.of(model.products)

        // Assert
        assertThat(productSet?.value).isEqualTo(model.expectedValue)
    }

    @ParameterizedTest
    @MethodSource("provideOrderErrorModels")
    fun `GIVEN order error WHEN order failed event created THEN error code matches the spec`(model: OrderErrorModel) {
        // Act
        val event = TangemPayAnalyticsEvents.OnboardingOrderFailed(error = model.error, availableProducts = emptySet())

        // Assert
        assertThat(event.id).isEqualTo("[Visa Onboarding] Onboarding Order Failed")
        assertThat(event.params).isEqualTo(mapOf("error_code" to model.expectedCode))
    }

    internal data class OrderErrorModel(val error: VisaApiError, val expectedCode: String)

    internal data class EventModel(
        val event: TangemPayAnalyticsEvents,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )

    internal data class ProductSetModel(val products: Set<TangemPayProduct>, val expectedValue: String?)

    private fun provideTestModels() = listOf(
        EventModel(
            event = TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.CARD),
            expectedId = "[Visa Screen] Button - Terms And Limits",
            expectedParams = mapOf("product" to "card"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.ACCOUNT),
            expectedId = "[Visa Screen] Button - Terms And Limits",
            expectedParams = mapOf("product" to "account"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.TransactionInListClicked(
                product = TangemPayAnalyticsEvents.Product.CARD,
                type = "spend",
                status = "declined",
            ),
            expectedId = "[Visa Screen] Clicked On Transaction In List",
            expectedParams = mapOf("product" to "card", "type" to "spend", "status" to "declined"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.TransactionInListClicked(
                product = TangemPayAnalyticsEvents.Product.ACCOUNT,
                type = "deposit",
                status = "completed",
            ),
            expectedId = "[Visa Screen] Clicked On Transaction In List",
            expectedParams = mapOf("product" to "account", "type" to "deposit", "status" to "completed"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.CurrentPlanClicked(),
            expectedId = "[Visa Tiers] Current Plan Clicked",
            expectedParams = mapOf("source" to "payment_account"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.ChangePlanClicked(),
            expectedId = "[Visa Tiers] Change Plan Clicked",
            expectedParams = mapOf("source" to "payment_account"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.ActivationScreenOpened(
                entryChannel = TangemPayAnalyticsEvents.EntryChannel.BANNER,
            ),
            expectedId = "[Visa Onboarding] Visa Activation Screen Opened",
            expectedParams = mapOf(
                "entry_intent" to "generic",
                "entry_channel" to "banner",
                "first_product_listed" to "card",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.ActivationScreenOpened(entryChannel = null),
            expectedId = "[Visa Onboarding] Visa Activation Screen Opened",
            expectedParams = mapOf("entry_intent" to "generic", "first_product_listed" to "card"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.GetCardClicked(entryChannel = TangemPayAnalyticsEvents.EntryChannel.DEEPLINK),
            expectedId = "[Visa Onboarding] Button - Visa Get Card",
            expectedParams = mapOf("entry_intent" to "generic", "entry_channel" to "deeplink"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.GetCardClicked(entryChannel = null),
            expectedId = "[Visa Onboarding] Button - Visa Get Card",
            expectedParams = mapOf("entry_intent" to "generic"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.ViewTermsClicked(),
            expectedId = "[Visa Onboarding] Button - Visa View Terms",
            expectedParams = mapOf("link" to "terms"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.KycFlowOpened(),
            expectedId = "[Visa Onboarding] Visa KYC Flow Opened",
            expectedParams = mapOf("entry_intent" to "generic"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.PermanentBannerShowed(),
            expectedId = "[Visa Onboarding] Visa Permanent Banner Showed",
            expectedParams = mapOf("entry_intent" to "generic", "entry_channel" to "banner"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.MainVisaPermanentBannerClicked(),
            expectedId = "[Visa Onboarding] Visa Permanent Banner Clicked",
            expectedParams = mapOf("entry_intent" to "generic", "entry_channel" to "banner"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.PermanentButtonShowed(),
            expectedId = "[Visa Onboarding] Visa Permanent Button Showed",
            expectedParams = mapOf("entry_intent" to "generic", "entry_channel" to "details"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.DetailsVisaPermanentButtonClicked(),
            expectedId = "[Visa Onboarding] Visa Permanent Button Clicked",
            expectedParams = mapOf("entry_intent" to "generic", "entry_channel" to "details"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult(
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
                country = "United States",
                state = "Texas",
            ),
            expectedId = "[VA Onboarding] VA Eligibility Check Result",
            expectedParams = mapOf(
                "result" to "eligible",
                "card_eligible" to "yes",
                "account_eligible" to "yes",
                "country" to "United States",
                "state" to "Texas",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult(
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
                country = "Germany",
                state = null,
            ),
            expectedId = "[VA Onboarding] VA Eligibility Check Result",
            expectedParams = mapOf(
                "result" to "eligible",
                "card_eligible" to "yes",
                "account_eligible" to "no",
                "country" to "Germany",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult(
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
                country = "Germany",
                state = null,
            ),
            expectedId = "[VA Onboarding] VA Eligibility Check Result",
            expectedParams = mapOf(
                "result" to "eligible",
                "card_eligible" to "no",
                "account_eligible" to "yes",
                "country" to "Germany",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult(
                availableProducts = emptySet(),
                country = "Germany",
                state = null,
            ),
            expectedId = "[VA Onboarding] VA Eligibility Check Result",
            expectedParams = mapOf(
                "result" to "not_eligible",
                "card_eligible" to "no",
                "account_eligible" to "no",
                "country" to "Germany",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed(
                products = TangemPayAnalyticsEvents.ProductSet.CARD_AND_ACCOUNT,
            ),
            expectedId = "[Visa Tiers] Tier Selection Screen Showed",
            expectedParams = mapOf("products" to "card+account", "consent_line_shown" to "no"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed(products = null),
            expectedId = "[Visa Tiers] Tier Selection Screen Showed",
            expectedParams = mapOf("consent_line_shown" to "no"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.PlanSelectedClick(
                tierId = "PLUS",
                products = TangemPayAnalyticsEvents.ProductSet.CARD,
                context = TangemPayAnalyticsEvents.Tiers.PlanContext.ONBOARDING,
            ),
            expectedId = "[Visa Tiers] Plan Selected Click",
            expectedParams = mapOf("Plan" to "PLUS", "products" to "card", "context" to "onboarding"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.PlanSelectedClick(
                tierId = "BASIC",
                products = null,
                context = TangemPayAnalyticsEvents.Tiers.PlanContext.CHANGE_PLAN,
            ),
            expectedId = "[Visa Tiers] Plan Selected Click",
            expectedParams = mapOf("Plan" to "BASIC", "context" to "change_plan"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.CancelPlusMoveToBasicClicked(),
            expectedId = "[Visa Tiers] Cancel Plus Move To Basic Clicked",
            expectedParams = mapOf("stage" to "before_deposit"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.VerificationResultReceived(
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
            ),
            expectedId = "[Visa Onboarding] Verification Result Received",
            expectedParams = mapOf(
                "products_available" to "card+account",
                "card_available" to "yes",
                "account_available" to "yes",
                "entry_intent" to "generic",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.VerificationResultReceived(availableProducts = emptySet()),
            expectedId = "[Visa Onboarding] Verification Result Received",
            expectedParams = mapOf(
                "products_available" to "none",
                "card_available" to "no",
                "account_available" to "no",
                "entry_intent" to "generic",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.EligibilityRetryScreenOpened(
                stage = TangemPayAnalyticsEvents.EligibilityStage.EARLY_FILTER,
            ),
            expectedId = "[Visa Onboarding] Eligibility Retry Screen Opened",
            expectedParams = mapOf("stage" to "early_filter"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.EligibilityRetryScreenOpened(
                stage = TangemPayAnalyticsEvents.EligibilityStage.AFTER_VERIFICATION,
            ),
            expectedId = "[Visa Onboarding] Eligibility Retry Screen Opened",
            expectedParams = mapOf("stage" to "after_verification"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.WalletSignatureRequested(
                walletType = TangemPayAnalyticsEvents.WalletType.CARD,
            ),
            expectedId = "[Visa Onboarding] Wallet Signature Requested",
            expectedParams = mapOf("wallet_type" to "card"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.WalletSignatureRequested(
                walletType = TangemPayAnalyticsEvents.WalletType.HOT,
            ),
            expectedId = "[Visa Onboarding] Wallet Signature Requested",
            expectedParams = mapOf("wallet_type" to "hot"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.WalletSignatureFinished(
                result = TangemPayAnalyticsEvents.SignatureResult.SIGNED,
                walletType = TangemPayAnalyticsEvents.WalletType.CARD,
            ),
            expectedId = "[Visa Onboarding] Wallet Signature Finished",
            expectedParams = mapOf("result" to "signed", "wallet_type" to "card"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.WalletSignatureFinished(
                result = TangemPayAnalyticsEvents.SignatureResult.CANCELLED,
                walletType = TangemPayAnalyticsEvents.WalletType.HOT,
            ),
            expectedId = "[Visa Onboarding] Wallet Signature Finished",
            expectedParams = mapOf("result" to "cancelled", "wallet_type" to "hot"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.WalletSignatureFinished(
                result = TangemPayAnalyticsEvents.SignatureResult.FAILED,
                walletType = TangemPayAnalyticsEvents.WalletType.CARD,
            ),
            expectedId = "[Visa Onboarding] Wallet Signature Finished",
            expectedParams = mapOf("result" to "failed", "wallet_type" to "card"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.OnboardingOrderCreated(
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
                plan = tariffPlan(isBasicTier = false),
            ),
            expectedId = "[Visa Onboarding] Onboarding Order Created",
            expectedParams = mapOf(
                "products" to "card+account",
                "plan" to "plus",
                "order_type" to "onboarding",
                "is_existing_holder" to "no",
                "entry_intent" to "generic",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.OnboardingOrderCreated(
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
                plan = tariffPlan(isBasicTier = true),
            ),
            expectedId = "[Visa Onboarding] Onboarding Order Created",
            expectedParams = mapOf(
                "products" to "card",
                "plan" to "basic",
                "order_type" to "onboarding",
                "is_existing_holder" to "no",
                "entry_intent" to "generic",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.OnboardingOrderCreated(
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
                plan = null,
            ),
            expectedId = "[Visa Onboarding] Onboarding Order Created",
            expectedParams = mapOf(
                "products" to "account",
                "order_type" to "onboarding",
                "is_existing_holder" to "no",
                "entry_intent" to "generic",
            ),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.OnboardingOrderFailed(
                error = VisaApiError.CardIssueActiveOrderExists,
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
            ),
            expectedId = "[Visa Onboarding] Onboarding Order Failed",
            expectedParams = mapOf("error_code" to "active_order_exists", "products" to "card+account"),
        ),
    )

    private fun provideOrderErrorModels() = listOf(
        OrderErrorModel(error = VisaApiError.CardIssueOfferNotAvailable, expectedCode = "not_eligible"),
        OrderErrorModel(error = VisaApiError.CustomerIsBlocked, expectedCode = "not_eligible"),
        OrderErrorModel(error = VisaApiError.CardIssueActiveOrderExists, expectedCode = "active_order_exists"),
        OrderErrorModel(error = VisaApiError.AccountIssueActiveOrderExists, expectedCode = "active_order_exists"),
        OrderErrorModel(error = VisaApiError.ProductInstanceIsAlreadyActivated, expectedCode = "already_onboarded"),
        OrderErrorModel(error = VisaApiError.ServerUnavailable, expectedCode = "network"),
        OrderErrorModel(error = VisaApiError.UnknownWithoutCode, expectedCode = "network"),
        OrderErrorModel(error = VisaApiError.Unspecified, expectedCode = "other"),
        OrderErrorModel(error = VisaApiError.Unknown(errorCode = 104130100), expectedCode = "other"),
    )

    private fun tariffPlan(isBasicTier: Boolean) = TangemPayTariffPlan(
        id = "plan",
        tierId = if (isBasicTier) "BASIC" else "PLUS",
        isBasicTier = isBasicTier,
        name = "Plan",
        programName = "program",
        descriptionItems = emptyList(),
    )

    private fun provideProductSetModels() = listOf(
        ProductSetModel(
            products = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
            expectedValue = "card+account",
        ),
        ProductSetModel(products = setOf(TangemPayProduct.PAYMENT_ACCOUNT), expectedValue = "card"),
        ProductSetModel(products = setOf(TangemPayProduct.BANKING_ACCOUNT), expectedValue = "account"),
        ProductSetModel(products = emptySet(), expectedValue = null),
    )
}