package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

/** Welcome screen: its bottom sheet rows carry no test tag, hence the text matching. */
class IntroductionPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<IntroductionPageObject>(semanticsProvider = semanticsProvider) {

    val createWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.introduction_button_create_wallet))
        useUnmergedTree = true
    }

    val iHaveWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.introduction_button_i_have_a_wallet))
        useUnmergedTree = true
    }

    /** Scans an existing card in the 'I have a wallet' sheet, creates one in the 'Create wallet' sheet. */
    val cardOrRingOption: KNode = child {
        hasText(getResourceString(R.string.introduction_create_wallet_hardware_title))
        useUnmergedTree = true
    }

    val createSeedPhraseWalletOption: KNode = child {
        hasText(getResourceString(R.string.introduction_create_wallet_seed_phrase_title))
        useUnmergedTree = true
    }

    val importWalletOption: KNode = child {
        hasText(getResourceString(R.string.hw_import_existing_wallet))
        useUnmergedTree = true
    }

    val useRecoveryPhraseButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.hw_cloud_backup_restore_use_recovery_phrase))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onIntroductionScreen(function: IntroductionPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)