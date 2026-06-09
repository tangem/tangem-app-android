package com.tangem.features.onramp.utils

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.onramp.impl.R

/**
 * Onramp (buy) is disabled for demo cards. When [userWallet] is a demo cold wallet, shows the
 * demo-mode warning dialog and returns `true` — callers MUST abort the buy action in that case.
 *
 * Returns `false` for any non-demo wallet, so the caller can proceed.
 */
internal fun UiMessageSender.showDemoModeWarningIfNeeded(
    userWallet: UserWallet,
    isDemoCardUseCase: IsDemoCardUseCase,
): Boolean {
    val isDemo = userWallet is UserWallet.Cold && isDemoCardUseCase(cardId = userWallet.cardId)
    if (isDemo) {
        send(
            DialogMessage(
                title = resourceReference(id = R.string.warning_demo_mode_title),
                message = resourceReference(id = R.string.warning_demo_mode_message),
            ),
        )
    }
    return isDemo
}