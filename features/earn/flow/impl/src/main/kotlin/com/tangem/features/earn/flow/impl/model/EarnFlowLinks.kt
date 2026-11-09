package com.tangem.features.earn.flow.impl.model

import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class EarnFlowLinks @Inject constructor(
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
) {

    /** `null` when the network has no explorer for [ref]: the result screen then hides its explorer buttons. */
    fun explorerUrl(ref: EarnTxRef, currency: CryptoCurrency): String? =
        getExplorerTransactionUrlUseCase(txHash = ref.id, currency = currency).fold(
            ifLeft = { error ->
                TangemLogger.e("Earn flow: no explorer link for ${ref.id}: $error")
                null
            },
            ifRight = { it },
        )

    fun open(url: String) {
        urlOpener.openUrl(url)
    }

    fun share(url: String) {
        shareManager.shareText(url)
    }
}