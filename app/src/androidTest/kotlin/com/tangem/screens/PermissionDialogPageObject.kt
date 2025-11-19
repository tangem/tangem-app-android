package com.tangem.screens

import com.kaspersky.components.kautomator.component.text.UiButton
import com.kaspersky.components.kautomator.component.text.UiTextView
import com.kaspersky.components.kautomator.screen.UiScreen

object PermissionDialogPageObject : UiScreen<PermissionDialogPageObject>() {

    override val packageName = "com.android.permissioncontroller"

    val allowWhileUsingButton = UiButton {
        withId(this@PermissionDialogPageObject.packageName,"permission_allow_foreground_only_button")
    }

    val allowOnlyThisTimeButton = UiButton {
        withId(this@PermissionDialogPageObject.packageName, "permission_allow_one_time_button")
    }

    val doNotAllowButton = UiButton {
        withId(this@PermissionDialogPageObject.packageName, "permission_deny_button")
    }

    val permissionMessage = UiTextView {
        withId(this@PermissionDialogPageObject.packageName, "permission_message")
    }
}