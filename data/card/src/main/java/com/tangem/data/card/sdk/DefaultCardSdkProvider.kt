package com.tangem.data.card.sdk

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.CardFilter
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.bip39.Wordlist
import com.tangem.sdk.DefaultSessionViewDelegate
import com.tangem.sdk.extensions.*
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.storage.create
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CardSDK instance provider
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DefaultCardSdkProvider @Inject constructor() : CardSdkProvider, CardSdkOwner {

    override val sdk: TangemSdk
        get() = requireNotNull(value = holder?.sdk) {
            "Impossible to get the TangemSdk when activity is destroyed"
        }

    private val observer: LifecycleObserver = Observer()

    private var holder: Holder? = null

    override fun register(activity: FragmentActivity) {
        Log.info { "Tangem SDK owner registered" }

        if (holder != null) {
            unsubscribeAndCleanup()
        }

        initialize(activity)

        activity.lifecycle.addObserver(observer)
    }

    private fun initialize(activity: FragmentActivity) {
        val secureStorage = SecureStorage.create(activity)
        val nfcManager = TangemSdk.initNfcManager(activity)
        val authenticationManager = TangemSdk.initAuthenticationManager(activity)
        val keystoreManager = TangemSdk.initKeystoreManager(authenticationManager, secureStorage)

        val viewDelegate = DefaultSessionViewDelegate(nfcManager, activity)
        viewDelegate.sdkConfig = config

        val sdk = TangemSdk(
            reader = nfcManager.reader,
            viewDelegate = viewDelegate,
            secureStorage = secureStorage,
            authenticationManager = authenticationManager,
            keystoreManager = keystoreManager,
            wordlist = Wordlist.getWordlist(activity),
            config = config,
        )

        holder = Holder(
            activity = activity,
            nfcManager = nfcManager,
            authenticationManager = authenticationManager,
            sdk = sdk,
        )

        Log.info { "Tangem SDK initialized" }
    }

    private fun unsubscribeAndCleanup() {
        with(receiver = holder ?: return) {
            nfcManager.unsubscribe(activity)
            authenticationManager.unsubscribe(activity)

            activity.lifecycle.removeObserver(observer)
        }

        holder = null

        Log.info { "Tangem SDK unsubscribed and cleaned up" }
    }

    inner class Observer : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            Log.info { "Tangem SDK owner destroyed" }

            unsubscribeAndCleanup()
        }
    }

    data class Holder(
        val activity: FragmentActivity,
        val sdk: TangemSdk,
        val nfcManager: NfcManager,
        val authenticationManager: AuthenticationManager,
    )

    private companion object {

        val config = Config(
            linkedTerminal = true,
            allowUntrustedCards = true,
            filter = CardFilter(
                allowedCardTypes = FirmwareVersion.FirmwareType.entries.toList(),
                maxFirmwareVersion = FirmwareVersion(major = 6, minor = 33),
                batchIdFilter = CardFilter.Companion.ItemFilter.Deny(
                    items = setOf("0027", "0030", "0031", "0035"),
                ),
            ),
        )
    }
}