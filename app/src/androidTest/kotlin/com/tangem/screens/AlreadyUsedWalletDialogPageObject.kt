package com.tangem.screens

import com.tangem.common.extensions.withDialogRoot
import com.tangem.wallet.R
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

object AlreadyUsedWalletDialogPageObject : BaseDialog() {

    val title = KTextView {
        withText(R.string.security_alert_title)
    }.withDialogRoot()

    val message = KTextView {
        withText(R.string.wallet_been_activated_message)
    }.withDialogRoot()

    val cancelButton = KButton {
        withText(R.string.common_cancel)
    }.withDialogRoot()

    val requestSupportButton = KButton {
        withText(R.string.alert_button_request_support)
    }.withDialogRoot()

    val thisIsMyWalletButton = KButton {
        withText(R.string.this_is_my_wallet_title)
    }.withDialogRoot()
}