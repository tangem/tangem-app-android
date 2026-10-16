package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TransactionSuccessScreenTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class OnrampSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<OnrampSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.common_in_progress))
        useUnmergedTree = true
    }

    val currencyIcon: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.CURRENCY_ICON)
        useUnmergedTree = true
    }

    val providerIcon: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.PROVIDER_ICON)
        useUnmergedTree = true
    }

    val footer: KNode = child {
        hasText(getResourceString(R.string.onramp_transaction_status_footer_text))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_close))
        useUnmergedTree = true
    }

    val awaitingDepositStatus: KNode = child {
        hasText(getResourceString(CoreResR.string.express_exchange_status_receiving_active))
        useUnmergedTree = true
    }

    fun textContaining(text: String): KNode = child {
        hasText(text, substring = true)
        useUnmergedTree = true
    }

    fun providerName(name: String): KNode = child {
        hasText(name)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onOnrampSuccessScreen(function: OnrampSuccessPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)