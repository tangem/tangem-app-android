package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.feature.qrscanning.impl.R as QrScanningImplR

class CameraAccessDeniedBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<CameraAccessDeniedBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(QrScanningImplR.string.qr_scanner_camera_denied_title))
        useUnmergedTree = true
    }

    val subtitle: KNode = child {
        hasText(getResourceString(QrScanningImplR.string.qr_scanner_camera_denied_text))
        useUnmergedTree = true
    }

    val settingsButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.qr_scanner_camera_denied_settings_button)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val selectFromGalleryButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.qr_scanner_camera_denied_gallery_button)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_close)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onCameraAccessDeniedBottomSheet(function: CameraAccessDeniedBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)