package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.DeviceSettingsScreenTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class DeviceSettingsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DeviceSettingsPageObject>(semanticsProvider = semanticsProvider) {

    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(DeviceSettingsScreenTestTags.LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val imageBlock: KNode = child {
        hasTestTag(DeviceSettingsScreenTestTags.IMAGE_BLOCK)
        useUnmergedTree = true
    }

    val resetToFactorySettingsButtonTitle: KNode = child {
        hasTestTag(DeviceSettingsScreenTestTags.ITEM_TITLE)
        hasText(getResourceString(R.string.card_settings_reset_card_to_factory))
        useUnmergedTree = true
    }

    val scanCardOrRingButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.scan_card_settings_button))
        useUnmergedTree = true
    }

    fun resetToFactorySettingsButtonSubtitle(withBackup: Boolean = false): KNode = child {
        hasTestTag(DeviceSettingsScreenTestTags.ITEM_SUBTITLE)
        useUnmergedTree = true
        if (withBackup) {
            hasText(getResourceString(R.string.reset_card_with_backup_to_factory_message))
        } else {
            hasText(getResourceString(R.string.reset_card_without_backup_to_factory_message))
        }
    }
}

internal fun BaseTestCase.onDeviceSettingsScreen(function: DeviceSettingsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)