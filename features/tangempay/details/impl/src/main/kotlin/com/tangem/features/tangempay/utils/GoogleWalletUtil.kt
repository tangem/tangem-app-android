package com.tangem.features.tangempay.utils

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "GoogleWalletUtil"
private const val WALLET_PACKAGE_NAME = "com.google.android.apps.walletnfcrel"

internal class GoogleWalletUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var walletIntent: Intent? = null

    fun isWalletAvailable(): Boolean = getWalletIntent() != null

    fun openWallet() {
        val intent = getWalletIntent()
        if (intent != null) {
            context.startActivity(intent)
        }
    }

    private fun getWalletIntent(): Intent? {
        return if (walletIntent != null) {
            walletIntent
        } else {
            try {
                context.packageManager.getLaunchIntentForPackage(WALLET_PACKAGE_NAME)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    .also { walletIntent = it }
            } catch (exception: Exception) {
                Timber.tag(TAG).e(exception)
                null
            }
        }
    }
}