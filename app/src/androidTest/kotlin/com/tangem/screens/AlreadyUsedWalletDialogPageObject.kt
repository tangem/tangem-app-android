package com.tangem.screens

import com.kaspersky.kaspresso.screens.KScreen
import com.tangem.wallet.R
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.KButton

object AlreadyUsedWalletDialogPageObject : KScreen<AlreadyUsedWalletDialogPageObject>() {

    override val layoutId: Int? = null
    override val viewClass: Class<*>? = null

    val title = KTextView {
        withText(R.string.security_alert_title)
    }

    val message = KTextView {
        withText(R.string.wallet_been_activated_message)
    }

    val cancelButton = KButton {
        withText(R.string.common_cancel)
    }

    val requestSupportButton = KButton {
        withText(R.string.alert_button_request_support)
    }

    val thisIsMyWalletButton = KButton {
        withText(R.string.this_is_my_wallet_title)
    }
}