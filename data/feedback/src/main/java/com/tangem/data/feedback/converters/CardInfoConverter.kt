package com.tangem.data.feedback.converters

import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.converter.Converter

/**
 * Converter from [UserWallet] to [CardInfo]
 *
* [REDACTED_AUTHOR]
 */
internal object CardInfoConverter : Converter<UserWallet, CardInfo> {

    override fun convert(value: UserWallet): CardInfo {
        return with(value.scanResponse) {
            CardInfo(
                cardId = card.cardId,
                firmwareVersion = card.firmwareVersion.stringValue,
                cardBlockchain = walletData?.blockchain,
                signedHashesList = card.wallets.map {
                    CardInfo.SignedHashes(curve = it.curve.curve, total = it.totalSignedHashes?.toString())
                },
                isImported = value.isImported,
                isStart2Coin = value.scanResponse.card.isStart2Coin,
            )
        }
    }
}
