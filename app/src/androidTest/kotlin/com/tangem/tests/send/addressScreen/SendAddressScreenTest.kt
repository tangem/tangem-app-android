package com.tangem.tests.send.addressScreen

import android.Manifest
import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ENS_ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.ENS_ETHEREUM_RECIPIENT_SHORTENED_ADDRESS
import com.tangem.common.constants.TestConstants.ENS_NAME
import com.tangem.common.constants.TestConstants.ETHEREUM_ADDRESS
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.XRP_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.XRP_X_ADDRESS
import com.tangem.common.constants.TestConstants.XRP_X_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.XRP_X_RECIPIENT_ADDRESS_WITH_TAG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.clearClipboard
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setClipboardText
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SendAddressScreenTest : BaseTestCase() {

    @AllureId("4004")
    @DisplayName("Send (address screen): check destination tag")
    @Test
    fun sendDestinationTagTest() {
        val tokenName = "XRP Ledger"
        val sendAmount = "1"
        val correctMemo = "123"
        val invalidMemo = "hz"
        val xrpRecipientAddress = XRP_RECIPIENT_ADDRESS
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val context = device.context

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Set clipboard text") {
                setClipboardText(context, correctMemo)
            }
            step("Assert 'Destination Tag' title is displayed") {
                onSendAddressScreen { destinationTagBlockTitle().assertIsDisplayed() }
            }
            step("Assert 'Destination Tag' text is displayed") {
                onSendAddressScreen { destinationTagBlockText.assertIsDisplayed() }
            }
            step("CLick on 'Paste' button") {
                onSendAddressScreen { destinationTagPasteButton.clickWithAssertion() }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(xrpRecipientAddress) }
            }
            step("Assert 'Next' button is enabled") {
                onSendAddressScreen { nextButton.assertIsEnabled() }
            }
            step("Click on 'Clear text field button'") {
                onSendAddressScreen { clearDestinationTagTextFieldButton.clickWithAssertion() }
            }
            step("Type invalid memo in input text field") {
                onSendAddressScreen { destinationTagTextField.performTextReplacement(invalidMemo) }
            }
            step("Assert 'Invalid memo' title is displayed") {
                onSendAddressScreen { destinationTagBlockTitle(isMemoCorrectOrEmpty = false).assertIsDisplayed() }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("4543")
    @DisplayName("Send (address screen): check address field")
    @Test
    fun sendAddressFieldTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"
        val recipientAddress = ETHEREUM_RECIPIENT_ADDRESS
        val invalidAddress = "s"
        val walletAddress = ETHEREUM_ADDRESS
        val context = device.context
        val recipient = getResourceString(R.string.send_recipient)
        val notAValidAddress = getResourceString(R.string.send_recipient_address_error)
        val sameAsWalletAddress = getResourceString(R.string.send_error_address_same_as_wallet)

        setupHooks(
            additionalAfterSection = {
                clearClipboard()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Set clipboard text") {
                setClipboardText(context, recipientAddress)
            }
            step("Click on 'Paste' button") {
                onSendAddressScreen { addressPasteButton.clickWithAssertion() }
            }
            step("Assert address text field contains correct recipient address") {
                onSendAddressScreen { addressTextField.assertTextContains(recipientAddress) }
            }
            step("Assert address text field title is displayed") {
                onSendAddressScreen { addressTextFieldTitle.assertTextContains(recipient) }
            }
            step("Assert 'Next' button is enabled") {
                onSendAddressScreen { nextButton.assertIsEnabled() }
            }
            step("Set clipboard text") {
                setClipboardText(context, invalidAddress)
            }
            step("Click on 'Cross' button") {
                onSendAddressScreen { clearTextFieldButton.clickWithAssertion() }
            }
            step("Click on 'Paste' button") {
                onSendAddressScreen { addressPasteButton.clickWithAssertion() }
            }
            step("Assert address text field contains invalid address") {
                onSendAddressScreen { addressTextField.assertTextContains(invalidAddress) }
            }
            step("Assert invalid address text field title is displayed") {
                onSendAddressScreen { addressTextFieldTitle.assertTextContains(notAValidAddress) }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
            step("Set clipboard text") {
                setClipboardText(context, walletAddress)
            }
            step("Click on 'Cross' button") {
                onSendAddressScreen { clearTextFieldButton.clickWithAssertion() }
            }
            step("Click on 'Paste' button") {
                onSendAddressScreen { addressPasteButton.clickWithAssertion() }
            }
            step("Assert address text field contains invalid address") {
                onSendAddressScreen { addressTextField.assertTextContains(walletAddress) }
            }
            step("Assert 'Address is the same as wallet address' error title is displayed") {
                onSendAddressScreen { addressTextFieldTitle.assertTextContains(sameAsWalletAddress) }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("4009")
    @DisplayName("Send (address screen): check ENS name")
    @Test
    fun sendEnsNameTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"
        val ensName = ENS_NAME
        val invalidEnsName = "l"
        val ensAddress = ENS_ETHEREUM_RECIPIENT_ADDRESS
        val ensShortenedAddress = ENS_ETHEREUM_RECIPIENT_SHORTENED_ADDRESS
        val scenarioName = "eth_call_api"
        val scenarioState = "EnsName"
        val notAValidAddress = getResourceString(R.string.send_recipient_address_error)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).performClick() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Type '$sendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(sendAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type ENS name: '$ensName' in text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(ensName) }
            }
            step("Assert ENS address displayed") {
                onSendAddressScreen { resolvedAddress.assertTextContains(ensAddress, substring = true) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert recipient address is displayed") {
                onSendConfirmScreen { recipientAddress(ensName).assertIsDisplayed() }
            }
            step("Assert blockchain address is displayed") {
                onSendConfirmScreen { blockchainAddress.assertTextContains(ensShortenedAddress, substring = true) }
            }
            step("Click on recipient address") {
                onSendConfirmScreen { recipientAddress(ensName).performClick() }
            }
            step("Click on 'Cross' button") {
                onSendAddressScreen { clearTextFieldButton.clickWithAssertion() }
            }
            step("Type invalid ENS name: '$invalidEnsName' in text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(invalidEnsName) }
            }
            step("Assert invalid address error title is displayed") {
                onSendAddressScreen { addressTextFieldTitle.assertTextContains(notAValidAddress) }
            }
        }
    }

    @AllureId("4574")
    @DisplayName("Send (address screen): check scan QR code screen")
    @Test
    fun checkQrCodeScreenTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"
        val packageName = BuildConfig.APPLICATION_ID
        val permissionName = Manifest.permission.CAMERA

        setupHooks(
            additionalBeforeSection = {
                device.uiDevice.executeShellCommand("pm grant $packageName $permissionName")
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Click on 'QR' button") {
                onSendAddressScreen { qrButton.clickWithAssertion() }
            }
            step("Check 'Scan QR code' screen") {
                checkScanQrScreen()
            }
        }
    }

    @AllureId("4576")
    @DisplayName("Send (address screen): give camera permission")
    @Test
    fun giveCameraPermissionScreenTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Click on 'QR' button") {
                onSendAddressScreen { qrButton.clickWithAssertion() }
            }
            step("Assert 'While using app' button is displayed") {
                waitForIdle()
                PermissionDialogPageObject { allowWhileUsingButton.isDisplayed() }
            }
            step("Assert 'Allow only this time' button is displayed") {
                PermissionDialogPageObject { allowOnlyThisTimeButton.isDisplayed() }
            }
            step("Assert 'Don't allow' button is displayed") {
                PermissionDialogPageObject { doNotAllowButton.isDisplayed() }
            }
            step("CLick on 'While using app' button") {
                PermissionDialogPageObject { allowWhileUsingButton.click() }
            }
            step("Check 'Scan QR code' screen") {
                checkScanQrScreen()
            }
        }
    }

    @AllureId("4578")
    @DisplayName("Send (address screen): don't give camera permission")
    @Test
    fun doNotGiveCameraPermissionScreenTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Click on 'QR' button") {
                onSendAddressScreen { qrButton.clickWithAssertion() }
            }
            step("Click on 'Don't allow' button") {
                waitForIdle()
                PermissionDialogPageObject { doNotAllowButton.click() }
            }
            step("Assert 'Camera access denied' bottom sheet title is displayed") {
                waitForIdle()
                onCameraAccessDeniedBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert 'Camera access denied' bottom sheet subtitle is displayed") {
                onCameraAccessDeniedBottomSheet { subtitle.assertIsDisplayed() }
            }
            step("Assert 'Settings' button is displayed") {
                onCameraAccessDeniedBottomSheet { settingsButton.assertIsDisplayed() }
            }
            step("Assert 'Select from the gallery' button is displayed") {
                onCameraAccessDeniedBottomSheet { selectFromGalleryButton.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onCameraAccessDeniedBottomSheet { closeButton.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onCameraAccessDeniedBottomSheet { closeButton.performClick() }
            }
            step("Assert 'Address' screen is displayed") {
                onSendAddressScreen { container.assertIsDisplayed() }
            }
        }
    }

    @AllureId("4590")
    @DisplayName("Send (address screen): enter 'r' address (XRP)")
    @Test
    fun xrpEnterRAddressTest() {
        val tokenName = "XRP Ledger"
        val sendAmount = "1"
        val xrpRecipientAddress = XRP_RECIPIENT_ADDRESS
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val optionalText = getResourceString(R.string.send_optional_field)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(xrpRecipientAddress) }
            }
            step("Check 'Destination tag' block") {
                checkDestinationTagBlock(hint = optionalText)
            }
        }
    }

    @AllureId("4591")
    @DisplayName("Send (address screen): enter 'X' address without tag (XRP)")
    @Test
    fun xrpEnterXAddressWithoutTagTest() {
        val tokenName = "XRP Ledger"
        val sendAmount = "1"
        val xrpRecipientAddress = XRP_X_RECIPIENT_ADDRESS
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val tagAlreadyIncluded = getResourceString(R.string.send_additional_field_already_included)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(xrpRecipientAddress) }
            }
            step("Check 'Destination tag' block") {
                checkDestinationTagBlock(hint = tagAlreadyIncluded)
            }
            step("Assert 'Next' button is enabled") {
                onSendAddressScreen { nextButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("4592")
    @DisplayName("Send (address screen): enter 'X' address with tag (XRP)")
    @Test
    fun xrpEnterXAddressWithTagTest() {
        val tokenName = "XRP Ledger"
        val sendAmount = "1"
        val xrpRecipientAddress = XRP_X_RECIPIENT_ADDRESS_WITH_TAG
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val tagAlreadyIncluded = getResourceString(R.string.send_additional_field_already_included)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(xrpRecipientAddress) }
            }
            step("Check 'Destination tag' block") {
                checkDestinationTagBlock(hint = tagAlreadyIncluded)
            }
            step("Assert 'Next' button is enabled") {
                onSendAddressScreen { nextButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("4594")
    @DisplayName("Send (address screen): enter own address (XRP)")
    @Test
    fun xrpEnterOwnXAddressTest() {
        val tokenName = "XRP Ledger"
        val sendAmount = "1"
        val walletAddress = XRP_X_ADDRESS
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val sameAsWalletAddress = getResourceString(R.string.send_error_address_same_as_wallet)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(walletAddress) }
            }
            step("Assert 'Address is the same as wallet address' error title is displayed") {
                onSendAddressScreen { addressTextFieldTitle.assertTextContains(sameAsWalletAddress) }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("4593")
    @DisplayName("Send (address screen): enter tag before address (XRP)")
    @Test
    fun xrpEnterTagBeforeAddressTest() {
        val tokenName = "XRP Ledger"
        val sendAmount = "1"
        val xrpRecipientAddress = XRP_X_RECIPIENT_ADDRESS
        val destinationTag = "12345"
        val emptyText = ""
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val tagAlreadyIncluded = getResourceString(R.string.send_additional_field_already_included)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Type destination tag in input text field") {
                onSendAddressScreen { destinationTagTextField.performTextReplacement(destinationTag) }
            }
            step("Type X address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(xrpRecipientAddress) }
            }
            step("Destination tag text field doesn't contains destination tag: '$destinationTag'") {
                onSendAddressScreen { destinationTagTextField.assert (!hasText(destinationTag)) }
            }
            step("Destination tag text field cleared") {
                onSendAddressScreen { destinationTagTextField.assertTextEquals(emptyText) }
            }
            step("Check 'Destination tag' block") {
                checkDestinationTagBlock(hint = tagAlreadyIncluded)
            }
            step("Assert 'Next' button is enabled") {
                onSendAddressScreen { nextButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("4567")
    @DisplayName("Send (address screen): check address field")
    @Test
    fun checkSendAddressScreenTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"
        val hint = getResourceString(R.string.send_enter_address_field_ens)
        val recipient = getResourceString(R.string.send_recipient)

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'Address' screen title is displayed") {
                onSendAddressScreen { topAppBarTitle.assertIsDisplayed() }
            }
            step("Assert address text field title is displayed") {
                onSendAddressScreen { addressTextFieldTitle.assertTextContains(recipient) }
            }
            step("Assert address text field is displayed") {
                onSendAddressScreen { addressTextField.assertIsDisplayed() }
            }
            step("Assert address text field hint contains text '$hint'") {
                onSendAddressScreen { addressTextFieldHint.assertTextContains(hint) }
            }
            step("Assert 'QR' button is displayed") {
                onSendAddressScreen { qrButton.assertIsDisplayed() }
            }
            step("Assert 'Paste' button is displayed") {
                onSendAddressScreen { addressPasteButton.assertIsDisplayed() }
            }
            step("Assert recipient network caution is displayed for network '$tokenName'") {
                onSendAddressScreen { recipientNetworkCaution(tokenName).assertIsDisplayed() }
            }
            step("Assert 'Next' button is disabled") {
                onSendAddressScreen { nextButton.assertIsNotEnabled() }
            }
        }
    }
}