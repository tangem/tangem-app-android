package com.tangem

import android.app.Activity

object Constant {

    const val EXTRA_BLOCKCHAIN_DATA = "BLOCKCHAIN_DATA"

    const val EXTRA_MESSAGE = "message"

    const val EXTRA_MODIFICATION = "modification"
    const val EXTRA_MODIFICATION_DELETE = "delete"
    const val EXTRA_MODIFICATION_UPDATE = "update"

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

    // QrScanActivity
    const val EXTRA_QR_CODE = "QRCode"

    // PinSwapActivity
    const val EXTRA_CONFIRM_PIN = "confirmPIN"
    const val EXTRA_CONFIRM_PIN_2 = "confirmPIN2"
    const val EXTRA_NEW_PIN = "newPIN"
    const val EXTRA_NEW_PIN_2 = "newPIN2"

    // CreateNewWalletActivity
    const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER

    // EmptyWalletActivity
    const val REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY = 2
    const val REQUEST_CODE_REQUEST_PIN2 = 3

    // ConfirmPaymentActivity
    const val REQUEST_CODE_SIGN_PAYMENT = 1
    const val REQUEST_CODE_REQUEST_PIN2_ = 2

    // SendTransactionActivity
    const val EXTRA_TX: String = "TX"

    // SignPaymentActivity
    const val EXTRA_AMOUNT = "Amount"
    const val EXTRA_AMOUNT_CURRENCY = "AmountCurrency"
    const val EXTRA_FEE = "Fee"
    const val EXTRA_FEE_CURRENCY = "FeeCurrency"
    const val EXTRA_FEE_INCLUDED = "FeeIncluded"
    const val EXTRA_TARGET_ADDRESS = "TargetAddress"
    const val REQUEST_CODE_SEND_PAYMENT_ = 1
    const val RESULT_INVALID_PIN_ = Activity.RESULT_FIRST_USER

    // PreparePaymentActivity
    const val REQUEST_CODE_SCAN_QR = 1
    const val REQUEST_CODE_SEND_PAYMENT__ = 2

    // PrepareCryptonitOtherApiWithdrawalActivity
    const val REQUEST_CODE_SCAN_QR_KEY = 1
    const val REQUEST_CODE_SCAN_QR_SECRET = 2
    const val REQUEST_CODE_SCAN_QR_USER_ID = 3

}