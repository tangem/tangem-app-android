package com.tangem.screens

import com.kaspersky.kaspresso.screens.KScreen
import com.tangem.core.ui.R
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

object FailedCardVerificationDialogPageObject : KScreen<FailedCardVerificationDialogPageObject>() {

    override val layoutId: Int? = null
    override val viewClass: Class<*>? = null

    val title = KTextView {
        withId(R.id.alertTitle)
    }

    val message = KTextView {
        withId(R.id.message)
    }

    val cancelButton = KButton {
        withText(R.string.common_cancel)
    }

    val understandButton = KButton {
        withText(R.string.common_understand)
    }
}