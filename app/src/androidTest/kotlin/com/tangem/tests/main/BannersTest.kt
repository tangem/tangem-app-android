package com.tangem.tests.main

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.tangem.common.BaseTestCase
import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.domain.models.scan.ProductType.Note
import com.tangem.scenarios.addNewCardWallet
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.switchToPreviousWallet
import com.tangem.screens.onMainScreen
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test

@HiltAndroidTest
class BannersTest : BaseTestCase() {

    @AllureId("214")
    @DisplayName("Banners: Note banner contents link with pre-set promo code")
    @Test
    fun bannersNoteBannerContentsLinkWithPromoCodeTest() {
        val promoCodeUrl = TangemSiteUrlBuilder.NOTE_MIGRATION_URL

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = Note)
            }
            step("Stub external browser so the promo link is not actually opened") {
                intending(hasAction(Intent.ACTION_VIEW))
                    .respondWith(ActivityResult(Activity.RESULT_OK, null))
            }
            step("Assert 'Discover Tangem Wallet' banner is displayed") {
                onMainScreen { promoBannerTitle.assertIsDisplayed() }
            }
            step("Collapse header on Main screen") {
                onMainScreen { collapseHeader() }
            }
            step("Click on promo banner button") {
                onMainScreen { promoBannerButton.performClick() }
            }
            step("Assert promo link with pre-set promo code is opened: '$promoCodeUrl'") {
                intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(promoCodeUrl)))
            }
        }
    }

    @AllureId("4027")
    @DisplayName("Banners: Discover Tangem Wallet banner is not displayed if Wallet card added")
    @Test
    fun bannersDiscoverTangemBannerNotDisplayIfWalletAddedTest() {
        val walletName = "Note"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = Note)
            }
            step("Assert 'Discover Tangem Wallet' banner is displayed") {
                onMainScreen { promoBannerTitle.assertIsDisplayed() }
            }
            step("Add a second card wallet") {
                addNewCardWallet(WalletMockContent)
            }
            step("Switch back to the 'Note' wallet") {
                onMainScreen { switchToPreviousWallet() }
            }
            step("Assert 'Note' wallet is displayed") {
                onMainScreen { walletNameText.assertTextContains(walletName) }
            }
            step("Assert 'Discover Tangem Wallet' banner is not displayed ") {
                onMainScreen { promoBannerTitle.assertIsNotDisplayed() }
            }
        }
    }
}