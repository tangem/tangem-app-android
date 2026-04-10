package com.tangem.tests.accounts

import com.tangem.common.BaseTestCase
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AccountEditingTest : BaseTestCase() {

    @Test
    // @AllureId("5507")
    @DisplayName("Accounts: name field verifications when editing account in the wallet")
    fun accountsEditionNameFieldValidationTest() {

        // Launch App
        // Go to wallet settings
        // [MOCKED] found accounts - mocks already present
        //

    }

    @Test
    @AllureId("5505")
    @DisplayName("Accounts: check unsaved changes notification " +
        "after attempt to close edited account creation form")
    fun accountsCreationUnsavedChangesNotificationTest() {

        // Launch App
        // Go to wallet settings
        // [MOCKED] found accounts - mocks already present
        //

    }

    // TODO account: edit icon color (verify with scr auto)
    // TODO account: edit icon type (verify with scr auto)

    // TODO network error processing

    // TODO add token to account
    // TODO add several tokens to account
    // TODO delete token from account

}