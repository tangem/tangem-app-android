package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.SelectPaymentMethodBottomSheetTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class SelectPaymentMethodPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectPaymentMethodPageObject>(semanticsProvider = semanticsProvider) {


    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(SelectPaymentMethodBottomSheetTestTags.LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.onramp_pay_with))
    }

    fun paymentMethodWithNameAndIcon(name: String): KNode {
        return lazyList.child<KNode> {
            hasAnyDescendant(withText(name))
            hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_ICON))
            useUnmergedTree = true
        }
    }
}

internal fun BaseTestCase.onSelectPaymentMethodBottomSheet(function: SelectPaymentMethodPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)