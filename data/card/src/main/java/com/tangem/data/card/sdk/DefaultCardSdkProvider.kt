package com.tangem.data.card.sdk

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.tangem.TangemSdk
import com.tangem.common.CardFilter
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.Config
import com.tangem.sdk.extensions.initWithBiometrics
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CardSDK instance provider
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DefaultCardSdkProvider @Inject constructor() : CardSdkProvider, CardSdkLifecycleObserver {

    override val sdk: TangemSdk
        get() = requireNotNull(value = _sdk) { "Impossible to get the TangemSdk when activity is destroyed" }

    private var _sdk: TangemSdk? = null

    /** Weak reference of context that uses to create [TangemSdk] */
    private var contextRef: WeakReference<Context> = WeakReference(null)

    override fun onCreate(context: Context) {
        contextRef = WeakReference(context)
        _sdk = TangemSdk.initWithBiometrics(activity = context as FragmentActivity, config = config)
    }

    override fun onDestroy(context: Context) {
        /*


         */
        if (contextRef.get() == context) {
            _sdk = null
        }
    }

    private companion object {

        val config = Config(
            linkedTerminal = true,
            allowUntrustedCards = true,
            filter = CardFilter(
                allowedCardTypes = FirmwareVersion.FirmwareType.values().toList(),
                maxFirmwareVersion = FirmwareVersion(major = 6, minor = 33),
            ),
        )
    }
}