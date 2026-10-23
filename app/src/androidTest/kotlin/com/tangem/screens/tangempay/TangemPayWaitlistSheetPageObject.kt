package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.text.LinkAnnotation
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.intercept.operation.ComposeOperationType
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasAnyAncestor as withAnyAncestor
import androidx.compose.ui.test.hasSetTextAction as withSetTextAction
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.isFocused as withFocus

class TangemPayWaitlistSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayWaitlistSheetPageObject>(semanticsProvider = semanticsProvider) {

    private val privacyPolicyTitle = getResourceString(CoreResR.string.common_privacy_policy)

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_waitlist_title))
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_waitlist_description))
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val emailLabel: KNode = child {
        hasText(getResourceString(CoreResR.string.common_email))
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val emailField: KNode = child {
        addSemanticsMatcher(withSetTextAction())
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val emailPlaceholder: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_waitlist_email_placeholder))
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val emailError: KNode = child {
        hasText(getResourceString(CoreResR.string.common_error_email_incorrect_format))
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val marketingConsentCheckbox: KNode = child {
        isToggleable()
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val marketingConsentText: KNode = child {
        hasText(
            getResourceString(
                CoreResR.string.tangempay_waitlist_marketing_consent,
                getResourceString(CoreResR.string.common_privacy_policy),
            ),
        )
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    val saveButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_save))
        hasClickAction()
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
    }

    fun clickPrivacyPolicyLink() {
        marketingConsentText.delegate.perform(WaitlistAction.CLICK_PRIVACY_POLICY) {
            performFirstLinkClick { (it.item as? LinkAnnotation.Clickable)?.tag == privacyPolicyTitle }
        }
    }

    private enum class WaitlistAction : ComposeOperationType { CLICK_PRIVACY_POLICY }
}

val focusedWaitlistEmailFieldMatcher: SemanticsMatcher =
    withSetTextAction() and withFocus() and withAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))

internal fun BaseTestCase.onTangemPayWaitlistSheet(function: TangemPayWaitlistSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)