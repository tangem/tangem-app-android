package com.tangem.screens

import com.kaspersky.kaspresso.screens.KScreen
import com.tangem.wallet.R
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.KButton

object ScanWarningDialogPageObject : KScreen<ScanWarningDialogPageObject>() {

    override val layoutId: Int? = null
    override val viewClass: Class<*>? = null

    val warningTitle = KTextView {
        withText(R.string.common_warning)
    }

    val warningMessage = KTextView {
        withText(R.string.alert_troubleshooting_scan_card_title)
    }

    val tryAgainButton = KButton {
        withId(R.id.try_again_button)
    }

    val howToScanButton = KButton {
        withId(R.id.how_to_scan_button)
    }

    val requestSupportButton = KButton {
        withId(R.id.request_support_button)
    }

    val cancelButton = KButton {
        withId(R.id.cancel_button)
    }
}