package com.tangem

import android.app.Activity

object Constant {

    const val FLAVOR_TANGEM_ACCESS = "tangemAccess"
    const val FLAVOR_TANGEM_CARDANO = "tangemCardano"


    const val PREF_LAST_WALLET_ADDRESS = "last_wallet_address"

    const val URL_TANGEM = "https://play.google.com/store/apps/details?id=com.tangem.wallet"

    const val EXTRA_BLOCKCHAIN_DATA = "BLOCKCHAIN_DATA"

    const val WALLET_ADDRESS = "Wallet address"

    const val EXTRA_MESSAGE = "message"

    const val EXTRA_MODIFICATION = "modification"
    const val EXTRA_MODIFICATION_DELETE = "delete"
    const val EXTRA_MODIFICATION_UPDATE = "update"

    const val EXTRA_MODE = "mode"

    const val INTENT_TYPE_TEXT_PLAIN = "text/plain"

    // LoadedWalletFragment, VerifyCardFragment
    const val REQUEST_CODE_SEND_TRANSACTION = "REQUEST_CODE_SEND_TRANSACTION"
    const val REQUEST_CODE_PURGE = "REQUEST_CODE_PURGE"
    const val REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = "REQUEST_CODE_REQUEST_PIN2_FOR_PURGE"
    const val REQUEST_CODE_VERIFY_CARD = "REQUEST_CODE_VERIFY_CARD"
    const val REQUEST_CODE_ENTER_NEW_PIN = "REQUEST_CODE_ENTER_NEW_PIN"
    const val REQUEST_CODE_ENTER_NEW_PIN2 = "REQUEST_CODE_ENTER_NEW_PIN2"
    const val REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = "REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN"
    const val REQUEST_CODE_SWAP_PIN = "REQUEST_CODE_SWAP_PIN"
    const val REQUEST_CODE_RECEIVE_TRANSACTION = "REQUEST_CODE_RECEIVE_TRANSACTION"

    // MainFragment
    const val REQUEST_CODE_SHOW_CARD_ACTIVITY = "REQUEST_CODE_SHOW_CARD_ACTIVITY"
    const val REQUEST_CODE_ENTER_PIN_ACTIVITY = "REQUEST_CODE_ENTER_PIN_ACTIVITY"
    const val REQUEST_CODE_SEND_EMAIL = "REQUEST_CODE_SEND_EMAIL"
    const val REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS = 3

    const val EXTRA_LAST_DISCOVERED_TAG = "extra_last_tag"
    const val EXTRA_PIN2 = "PIN2"

    // LogoFragment
    const val EXTRA_AUTO_HIDE = "extra_auto_hide"
    const val MILLIS_AUTO_HIDE = 1000

    // PinRequestFragment

    const val KEY_ALIAS = "pinKey"
    const val KEYSTORE = "AndroidKeyStore"

    // QrScanFragment
    const val EXTRA_QR_CODE = "QRCode"

    // PinSwapFragment
    const val EXTRA_CONFIRM_PIN = "confirmPIN"
    const val EXTRA_CONFIRM_PIN_2 = "confirmPIN2"
    const val EXTRA_NEW_PIN = "newPIN"
    const val EXTRA_NEW_PIN_2 = "newPIN2"

    // CreateNewWalletFragment
    const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER

    // EmptyWalletFragment
    const val REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY = "REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY"
    const val REQUEST_CODE_REQUEST_PIN2 = "REQUEST_CODE_REQUEST_PIN2"

    // ConfirmTransactionFragment
    const val REQUEST_CODE_SIGN_TRANSACTION = "REQUEST_CODE_SIGN_TRANSACTION"
    const val REQUEST_CODE_REQUEST_PIN2_ = "REQUEST_CODE_REQUEST_PIN2_"

    // SendTransactionFragment
    const val EXTRA_TX: String = "TX"

    // SignTransactionFragment
    const val EXTRA_AMOUNT = "Amount"
    const val EXTRA_AMOUNT_CURRENCY = "AmountCurrency"
    const val EXTRA_FEE = "Fee"
    const val EXTRA_FEE_CURRENCY = "FeeCurrency"
    const val EXTRA_FEE_INCLUDED = "FeeIncluded"
    const val EXTRA_TARGET_ADDRESS = "TargetAddress"
    const val REQUEST_CODE_SEND_TRANSACTION_ = "REQUEST_CODE_SEND_TRANSACTION_"
    const val RESULT_INVALID_PIN_ = Activity.RESULT_FIRST_USER

    // PrepareTransactionFragment
    const val REQUEST_CODE_SCAN_QR = "REQUEST_CODE_SCAN_QR"
    const val REQUEST_CODE_SEND_TRANSACTION__ = "REQUEST_CODE_SEND_TRANSACTION__"

    // PrepareCryptonitOtherApiWithdrawalFragment
    const val REQUEST_CODE_SCAN_QR_KEY = "REQUEST_CODE_SCAN_QR_KEY"
    const val REQUEST_CODE_SCAN_QR_SECRET = "REQUEST_CODE_SCAN_QR_SECRET"
    const val REQUEST_CODE_SCAN_QR_USER_ID = "REQUEST_CODE_SCAN_QR_USER_ID"

}