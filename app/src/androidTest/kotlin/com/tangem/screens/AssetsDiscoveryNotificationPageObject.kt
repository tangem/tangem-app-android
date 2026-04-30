package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.WalletNotificationTestTags
import com.tangem.feature.wallet.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasAnyAncestor as withAnyAncestor
import androidx.compose.ui.test.hasTestTag as withTestTag

class AssetsDiscoveryNotificationPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AssetsDiscoveryNotificationPageObject>(semanticsProvider = semanticsProvider) {

    val container: KNode = child {
        hasTestTag(WalletNotificationTestTags.ASSETS_DISCOVERY_COMPLETED)
    }

    val title: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.initial_wallet_sync_banner_title))
        useUnmergedTree = true
    }

    val message: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(getResourceString(R.string.initial_wallet_sync_banner_description))
        useUnmergedTree = true
    }

    val manageTokensButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.main_manage_tokens))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        useUnmergedTree = true
        addSemanticsMatcher(
            withTestTag(NotificationTestTags.CLOSE_BUTTON)
                .and(withAnyAncestor(withTestTag(WalletNotificationTestTags.ASSETS_DISCOVERY_COMPLETED)))
        )
    }
}

internal fun BaseTestCase.onAssetsDiscoveryNotification(
    function: AssetsDiscoveryNotificationPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)