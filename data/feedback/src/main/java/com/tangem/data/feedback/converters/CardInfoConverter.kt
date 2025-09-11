package com.tangem.data.feedback.converters

import com.tangem.domain.card.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.card.common.TapWorkarounds.isVisa
import com.tangem.domain.card.common.util.getBackupCardsCount
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.utils.converter.Converter
import timber.log.Timber

/**
 * Converter from [ScanResponse] to [CardInfo]
 *
[REDACTED_AUTHOR]
 */
internal object CardInfoConverter : Converter<ScanResponse, CardInfo> {

    override fun convert(value: ScanResponse): CardInfo {
        Timber.e("CardInfoConverter: convert() called with scanResponse cardId: ${value.card.cardId}")

        return with(value) {
            Timber.e("CardInfoConverter: Starting conversion for card: ${card.cardId}")

            val userWalletId = createUserWalletId(scanResponse = value)
            Timber.e("CardInfoConverter: Created userWalletId: $userWalletId")

            val backupCardsCount = value.getBackupCardsCount()
            val cardsCountString = backupCardsCount?.toString() ?: "0"
            Timber.e("CardInfoConverter: Backup cards count: $backupCardsCount -> '$cardsCountString'")

            val firmwareVersion = card.firmwareVersion.stringValue
            Timber.e("CardInfoConverter: Firmware version: $firmwareVersion")

            val cardBlockchain = walletData?.blockchain
            Timber.e("CardInfoConverter: Card blockchain: $cardBlockchain")

            val walletsCount = card.wallets.size
            Timber.e("CardInfoConverter: Processing $walletsCount wallets for signed hashes")

            val signedHashesList = card.wallets.map { wallet ->
                val curve = wallet.curve.curve
                val totalSigned = wallet.totalSignedHashes?.toString()
                Timber.e("CardInfoConverter: Wallet curve: $curve, totalSignedHashes: $totalSigned")
                CardInfo.SignedHashes(curve = curve, total = totalSigned)
            }
            Timber.e("CardInfoConverter: Created ${signedHashesList.size} signed hashes entries")

            val isImported = value.card.wallets.any(CardDTO.Wallet::isImported)
            Timber.e("CardInfoConverter: isImported: $isImported")

            val isStart2CoinValue = value.card.isStart2Coin
            Timber.e("CardInfoConverter: isStart2Coin: $isStart2CoinValue")

            val isVisaValue = value.card.isVisa
            Timber.e("CardInfoConverter: isVisa: $isVisaValue")

            val cardInfo = CardInfo(
                userWalletId = userWalletId,
                cardId = card.cardId,
                cardsCount = cardsCountString,
                firmwareVersion = firmwareVersion,
                cardBlockchain = cardBlockchain,
                signedHashesList = signedHashesList,
                isImported = isImported,
                isStart2Coin = isStart2CoinValue,
                isVisa = isVisaValue,
            )

            Timber.e("CardInfoConverter: Conversion completed for cardId: ${card.cardId}")
            cardInfo
        }
    }

    private fun createUserWalletId(scanResponse: ScanResponse): UserWalletId? {
        Timber.e("CardInfoConverter: createUserWalletId() called for cardId: ${scanResponse.card.cardId}")

        val builder = UserWalletIdBuilder.scanResponse(scanResponse = scanResponse)
        Timber.e("CardInfoConverter: Created UserWalletIdBuilder")

        val userWalletId = builder.build()
        Timber.e("CardInfoConverter: Built userWalletId: $userWalletId")

        return userWalletId
    }
}