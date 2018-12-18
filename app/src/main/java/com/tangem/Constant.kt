package com.tangem

object Constant {

    const val EXTRA_BLOCKCHAIN_DATA = "BLOCKCHAIN_DATA"

    const val MESSAGE = "Message"
    const val ERROR = "Error"

    const val EXTRA_MODIFICATION = "modification"

    // LoadedWallet, VerifyCard
    const val REQUEST_CODE_SEND_PAYMENT = 1
    const val REQUEST_CODE_PURGE = 2
    const val REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = 3
    const val REQUEST_CODE_VERIFY_CARD = 4
    const val REQUEST_CODE_ENTER_NEW_PIN = 5
    const val REQUEST_CODE_ENTER_NEW_PIN2 = 6
    const val REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = 7
    const val REQUEST_CODE_SWAP_PIN = 8
    const val REQUEST_CODE_RECEIVE_PAYMENT = 9

    // MainActivity
    const val REQUEST_CODE_SHOW_CARD_ACTIVITY = 1
    const val REQUEST_CODE_ENTER_PIN_ACTIVITY = 2
    const val REQUEST_CODE_SEND_EMAIL = 3
    const val REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS = 3

    const val EXTRA_LAST_DISCOVERED_TAG = "extra_last_tag"
    const val EXTRA_PIN2 = "PIN2"

    // LogoActivity
    const val EXTRA_AUTO_HIDE = "extra_auto_hide"
    const val MILLIS_AUTO_HIDE = 1000

    // PinRequestActivity
    const val EXTRA_MODE = "mode"
    const val KEY_ALIAS = "pinKey"
    const val KEYSTORE = "AndroidKeyStore"

    // PinSwapActivity
    const val EXTRA_CONFIRM_PIN = "confirmPIN"
    const val EXTRA_CONFIRM_PIN_2 = "confirmPIN2"
    const val EXTRA_NEW_PIN = "newPIN"
    const val EXTRA_NEW_PIN_2 = "newPIN2"

}