package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.PopUpMenuTestTags
import io.github.kakaocup.compose.intercept.operation.ComposeOperationType
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

private val approvalSheet = withTestTag(BaseBottomSheetTestTags.CONTAINER) and
    withAnyDescendant(withText(getResourceString(R.string.give_permission_title)))

private val approveTypeMenu = withTestTag(PopUpMenuTestTags.CONTAINER)

private val learnMoreTitle = getResourceString(R.string.common_learn_more)

class SelectApprovalTypeBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectApprovalTypeBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.give_permission_title))
        hasAnyAncestor(approvalSheet)
        useUnmergedTree = true
    }

    val subtitle: KNode = child {
        hasText(getResourceString(R.string.give_permission_swap_subtitle_v2, "").trim(), substring = true)
        hasAnyAncestor(approvalSheet)
        useUnmergedTree = true
    }

    val learnMoreLink: KNode = child {
        hasText(learnMoreTitle, substring = true)
        hasAnyAncestor(approvalSheet)
        useUnmergedTree = true
    }

    val confirmButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_confirm)))
        hasAnyAncestor(approvalSheet)
        useUnmergedTree = true
    }

    fun amountRow(currencySymbol: String): KNode = child {
        hasText(getResourceString(R.string.give_permission_rows_amount, currencySymbol))
        hasAnyAncestor(approvalSheet)
        useUnmergedTree = true
    }

    fun selectedApproveType(currencySymbol: String, approveType: String): KNode = child {
        hasText(approveType)
        hasAnySibling(withText(getResourceString(R.string.give_permission_rows_amount, currencySymbol)))
        hasAnyAncestor(approvalSheet)
        useUnmergedTree = true
    }

    fun approveTypeMenuItem(approveType: String): KNode = child {
        hasText(approveType)
        hasAnyAncestor(approveTypeMenu)
        useUnmergedTree = true
    }

    fun approveTypeMenuCheckbox(approveType: String): KNode = child {
        addSemanticsMatcher(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
        hasAnySibling(withText(approveType))
        hasAnyAncestor(approveTypeMenu)
        useUnmergedTree = true
    }

    fun clickLearnMoreLink() {
        learnMoreLink.delegate.perform(LearnMoreAction.CLICK) {
            val node = fetchSemanticsNode()
            val text = node.config[SemanticsProperties.Text].joinToString(separator = "")
            val linkMiddleOffset = text.lastIndexOf(learnMoreTitle) + learnMoreTitle.length / 2
            val textLayouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(textLayouts)
            val linkCenter = textLayouts.first().getBoundingBox(linkMiddleOffset).center
            performTouchInput { click(linkCenter) }
        }
    }

    private enum class LearnMoreAction : ComposeOperationType { CLICK }
}

internal fun BaseTestCase.onSelectApprovalTypeBottomSheet(
    function: SelectApprovalTypeBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)