package com.tangem.screens

import com.kaspersky.kaspresso.screens.KScreen
import com.tangem.tap.features.disclaimer.ui.DisclaimerFragment
import com.tangem.wallet.R
import io.github.kakaocup.kakao.text.KButton

object DisclaimerScreen : KScreen<DisclaimerScreen>(){

    override val layoutId = R.layout.fragment_disclaimer

    override val viewClass = DisclaimerFragment::class.java

    val acceptButton: KButton = KButton {
        withId(R.id.btn_accept)
    }
}
