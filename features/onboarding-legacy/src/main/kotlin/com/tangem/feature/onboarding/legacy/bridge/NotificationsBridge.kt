package com.tangem.feature.onboarding.legacy.bridge

import com.tangem.sdk.api.TapError

interface NotificationsBridge {

    fun dispatchErrorNotification(error: TapError)

    fun dispatchErrorNotification(error: String)

    // TODO
    // fun dispatchDialogShow(dialog: Dialog)
}
