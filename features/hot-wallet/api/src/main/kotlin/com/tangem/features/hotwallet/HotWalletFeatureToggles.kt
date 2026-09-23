package com.tangem.features.hotwallet

interface HotWalletFeatureToggles {

    /** Whether Google Drive cloud backup is usable — requires Google Play Services on the device */
    val isGoogleDriveBackupEnabled: Boolean

    /** Whether the cloud backup password may be stored in and read back from the device password manager */
    val isPasswordManagerEnabled: Boolean
}