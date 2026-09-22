package com.tangem.features.hotwallet

interface HotWalletFeatureToggles {
    val isGoogleDriveBackupEnabled: Boolean

    /** Whether the cloud backup password may be stored in and read back from the device password manager */
    val isPasswordManagerEnabled: Boolean
}