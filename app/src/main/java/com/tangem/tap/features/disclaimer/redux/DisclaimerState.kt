package com.tangem.tap.features.disclaimer.redux

import android.net.Uri
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import org.rekotlin.StateType

data class DisclaimerState(
    val accepted: Boolean = false,
    val type: DisclaimerType = DisclaimerType.Tangem,
    val callback: DisclaimerCallback? = null,
) : StateType

sealed class DisclaimerType(
    val uri: Uri,
) {
    object Tangem : DisclaimerType(Uri.parse("https://tangem.com/tangem_tos.html"))
    object Start2Coin : DisclaimerType(Uri.parse("https://tangem.com/tangem_tos.html"))
    object SaltPay : DisclaimerType(Uri.parse("https://tangem.com/soltpay_tos.html"))

    companion object {
        fun get(scanResponse: ScanResponse): DisclaimerType = get(scanResponse.card)

        fun get(card: CardDTO): DisclaimerType = when {
            card.isSaltPay -> SaltPay
            card.isStart2Coin -> Start2Coin
            else -> Tangem
        }
    }
}
