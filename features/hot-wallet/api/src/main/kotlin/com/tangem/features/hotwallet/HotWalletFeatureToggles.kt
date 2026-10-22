package com.tangem.features.hotwallet

interface HotWalletFeatureToggles {

    /** Whether Google Drive cloud backup is usable — requires Google Play Services on the device */
    val isGoogleDriveBackupEnabled: Boolean
}