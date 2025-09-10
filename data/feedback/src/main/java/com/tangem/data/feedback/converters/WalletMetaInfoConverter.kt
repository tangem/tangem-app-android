package com.tangem.data.feedback.converters

import com.tangem.domain.card.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.card.common.TapWorkarounds.isVisa
import com.tangem.domain.card.common.util.getBackupCardsCount
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.utils.converter.Converter

/**
 * Converter from [UserWallet] to [WalletMetaInfo]
 *
[REDACTED_AUTHOR]
 */
internal object WalletMetaInfoConverter : Converter<UserWallet, WalletMetaInfo> {

    override fun convert(value: UserWallet): WalletMetaInfo {
        return when (value) {
            is UserWallet.Cold -> {
                WalletMetaInfo(
                    userWalletId = value.walletId,
                    cardId = value.scanResponse.card.cardId,
                    cardsCount = value.getBackupCardsCount()?.toString() ?: "0",
                    firmwareVersion = value.scanResponse.card.firmwareVersion.stringValue,
                    cardBlockchain = value.scanResponse.walletData?.blockchain,
                    signedHashesList = value.scanResponse.card.wallets.map {
                        WalletMetaInfo.SignedHashes(curve = it.curve.curve, total = it.totalSignedHashes?.toString())
                    },
                    isImported = value.scanResponse.card.wallets.any(CardDTO.Wallet::isImported),
                    isStart2Coin = value.scanResponse.card.isStart2Coin,
                    isVisa = value.scanResponse.card.isVisa,
                )
            }
            is UserWallet.Hot -> {
                WalletMetaInfo(
                    userWalletId = value.walletId,
                    hotWalletIsBackedUp = value.backedUp,
                )
            }
        }
    }

    fun convert(value: ScanResponse): WalletMetaInfo {
        return WalletMetaInfo(
            userWalletId = createUserWalletId(value),
            cardId = value.card.cardId,
            cardsCount = value.getBackupCardsCount()?.toString() ?: "0",
            firmwareVersion = value.card.firmwareVersion.stringValue,
            cardBlockchain = value.walletData?.blockchain,
            signedHashesList = value.card.wallets.map {
                WalletMetaInfo.SignedHashes(curve = it.curve.curve, total = it.totalSignedHashes?.toString())
            },
            isImported = value.card.wallets.any(CardDTO.Wallet::isImported),
            isStart2Coin = value.card.isStart2Coin,
            isVisa = value.card.isVisa,
        )
    }

    private fun createUserWalletId(scanResponse: ScanResponse): UserWalletId? {
        return UserWalletIdBuilder.scanResponse(scanResponse = scanResponse).build()
    }
}