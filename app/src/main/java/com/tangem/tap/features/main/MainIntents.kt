package com.tangem.tap.features.main

internal interface MainIntents {

    fun onHiddenBalanceToastAction()

    fun onShownBalanceToastAction()

    fun onHiddenBalanceNotificationAction(isPermanent: Boolean)

    fun onDismissBottomSheet()
}