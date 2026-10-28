package com.tangem.features.send.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface SendComponent : ComposableContentComponent {

    /**
     * @property intentId the MCP send intent this transfer fulfils. Its values stay editable: an intent opens Send
     *   like a QR scanned on the main screen, never in the locked sell-redirect mode.
     */
    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val transactionId: String? = null,
        val amount: String? = null,
        val tag: String? = null,
        val destinationAddress: String? = null,
        val entryType: EntryType = EntryType.Manual,
        val callback: ModelCallback? = null,
        val intentId: String? = null,
    )

    enum class EntryType {
        Manual,
        QR,
    }

    interface Factory : ComponentFactory<Params, SendComponent>

    interface ModelCallback {
        fun onConvertToAnotherToken(lastAmount: String, isEnterInFiatSelected: Boolean)
    }
}