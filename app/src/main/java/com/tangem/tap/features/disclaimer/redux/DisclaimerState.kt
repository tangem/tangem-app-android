package com.tangem.tap.features.disclaimer.redux

import android.net.Uri
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import org.rekotlin.StateType

data class DisclaimerState(
    val accepted: Boolean = false,
    val type: DisclaimerType = DisclaimerType.Tangem,
    val onAcceptCallback: VoidCallback? = null,
) : StateType

sealed class DisclaimerType(
    val uri: Uri,
) {
    object Tangem : DisclaimerType(Uri.parse("https://tangem.com/tangem_tos.html"))
    object SaltPay : DisclaimerType(Uri.parse("https://tangem.com/soltpay_tos.html"))

    companion object {
        fun get(scanResponse: ScanResponse): DisclaimerType = get(scanResponse.card)

        fun get(card: CardDTO): DisclaimerType = when {
            card.isSaltPay -> SaltPay
            else -> Tangem
        }
    }
}
