package com.tangem.screens

import com.kaspersky.components.kautomator.screen.UiScreen
import com.tangem.wallet.BuildConfig.APPLICATION_ID
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import com.tangem.wallet.R

class WalletConnectPopUpPageObject : UiScreen<WalletConnectPopUpPageObject>() {
    override val packageName = APPLICATION_ID

    val popUp = KView { withId(R.id.topPanel) }
    val title = KTextView { withId(R.id.alertTitle) }
    val message = KTextView { withId(android.R.id.message) }

    val rejectButton = KButton {
        withId(android.R.id.button2)
        withText("Reject")
    }

    val startButton = KButton {
        withId(android.R.id.button1)
        withText("Start")
    }
}