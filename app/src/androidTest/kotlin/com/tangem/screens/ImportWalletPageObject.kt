package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.ImportWalletScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class ImportWalletPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ImportWalletPageObject>(semanticsProvider = semanticsProvider) {

    val phraseTextField: KNode = child {
        hasTestTag(ImportWalletScreenTestTags.PHRASE_TEXT_FIELD)
        useUnmergedTree = true
    }

    val passphraseTextField: KNode = child {
        hasTestTag(ImportWalletScreenTestTags.PASSPHRASE_TEXT_FIELD)
        useUnmergedTree = true
    }

    val importButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyChild(withText(getResourceString(R.string.common_import)))
        useUnmergedTree = true
    }

    val continueButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyChild(withText(getResourceString(R.string.common_continue)))
        useUnmergedTree = true
    }

    val skipButton: KNode = child {
        hasTestTag(TopAppBarTestTags.MORE_BUTTON)
        hasText(getResourceString(R.string.common_skip))
        useUnmergedTree = true
    }

    val finishButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyChild(withText(getResourceString(R.string.common_finish)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onImportWalletScreen(function: ImportWalletPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)