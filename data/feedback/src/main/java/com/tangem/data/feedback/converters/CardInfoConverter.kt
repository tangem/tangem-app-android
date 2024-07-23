package com.tangem.data.feedback.converters

import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.converter.Converter

/**
 * Converter from [ScanResponse] to [CardInfo]
 *
[REDACTED_AUTHOR]
 */
internal object CardInfoConverter : Converter<ScanResponse, CardInfo> {

    override fun convert(value: ScanResponse): CardInfo {
        return with(value) {
            CardInfo(
                userWalletId = createUserWalletId(scanResponse = value),
                cardId = card.cardId,
                firmwareVersion = card.firmwareVersion.stringValue,
                cardBlockchain = walletData?.blockchain,
                signedHashesList = card.wallets.map {
                    CardInfo.SignedHashes(curve = it.curve.curve, total = it.totalSignedHashes?.toString())
                },
                isImported = value.card.wallets.any(CardDTO.Wallet::isImported),
                isStart2Coin = value.card.isStart2Coin,
            )
        }
    }

    private fun createUserWalletId(scanResponse: ScanResponse): UserWalletId? {
        return UserWalletIdBuilder.scanResponse(scanResponse = scanResponse).build()
    }
}