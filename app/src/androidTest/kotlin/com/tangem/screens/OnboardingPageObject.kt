package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.StoriesScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton
import com.tangem.features.onboarding.v2.impl.R as OnboardingImplR

class OnboardingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<OnboardingPageObject>(semanticsProvider = semanticsProvider) {

    val topBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val continueButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_continue))
        useUnmergedTree = true
    }

    val continueToMyWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.onboarding_button_continue_wallet))
        useUnmergedTree = true
    }

    val createWalletTopBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_header))
        useUnmergedTree = true
    }

    val createWalletTitle: KNode = child {
        hasTestTag(StoriesScreenTestTags.TITLE)
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_header))
        useUnmergedTree = true
    }

    val createWalletText: KNode = child {
        hasTestTag(StoriesScreenTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_body))
        useUnmergedTree = true
    }

    val gettingStartedTobBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(OnboardingImplR.string.onboarding_getting_started))
        useUnmergedTree = true
    }

    val creatingBackupTobBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(OnboardingImplR.string.onboarding_navbar_title_creating_backup))
        useUnmergedTree = true
    }

    val prepareYouCardOrRingTitle: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_title_scan_origin_card))
        useUnmergedTree = true
    }

    val startBackupText: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_subtitle_scan_primary))
        useUnmergedTree = true
    }

    val topBarMoreButton: KNode = child {
        hasTestTag(TopAppBarTestTags.MORE_BUTTON)
    }

    val backupWalletTitle: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_wallet_info_title_first))
        useUnmergedTree = true
    }

    val backupWalletText: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_wallet_info_subtitle_first))
        useUnmergedTree = true
    }

    val generateKeysPrivatelyTitle: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_options_title))
        useUnmergedTree = true
    }

    val generateKeysPrivatelyText: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_options_message))
        useUnmergedTree = true
    }

    val noBackupDevicesTitle: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_subtitle_no_backup_cards))
        useUnmergedTree = true
    }

    val noBackupDevicesText: KNode = child {
        hasText(getResourceString(OnboardingImplR.string.onboarding_title_no_backup_cards))
        useUnmergedTree = true
    }

    val createWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_button_create_wallet))
        useUnmergedTree = true
    }

    val backupWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_button_backup_now))
        useUnmergedTree = true
    }

    val scanCardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_button_backup_card))
        useUnmergedTree = true
    }

    val otherOptionsButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_create_wallet_options_button_options))
        useUnmergedTree = true
    }

    val addCardOrRingButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_button_add_backup_card))
        useUnmergedTree = true
    }

    val finalizeBackupButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_button_finalize_backup))
        useUnmergedTree = true
    }

    val skipForLaterButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.onboarding_button_skip_backup))
        useUnmergedTree = true
    }

    val enableNFCAlert: KView = KView {
        withId(R.id.alertTitle)
    }

    val cancelButton: KButton = KButton {
        withId(android.R.id.button2)
    }
}

internal fun BaseTestCase.onOnboardingScreen(function: OnboardingPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)