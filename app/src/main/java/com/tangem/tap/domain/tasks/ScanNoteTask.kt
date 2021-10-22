package com.tangem.tap.domain.tasks

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.toHexString
import com.tangem.operations.ScanTask
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.TwinsHelper

@Deprecated("Use ScanProductTask instead")
class ScanNoteTask(val card: Card? = null) : CardSessionRunnable<ScanResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        ScanTask().run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))

                is CompletionResult.Success -> {
                    val card = this.card ?: result.data

                    val error = getErrorIfExcludedCard(card)
                    if (error != null) {
                        callback(CompletionResult.Failure(error))
                        return@run
                    }

                    if (TwinsHelper.getTwinCardNumber(card.cardId) != null) {
                        dealWithTwinCard(card, session, callback)
                    } else if (!isTangemNote(card) && card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable) {
                        createMissingWalletsIfNeeded(card, session, callback)
                    } else {
                        callback(CompletionResult.Success(
                            ScanResponse(card, ProductType.Other, session.environment.walletData)))
                    }
                }
            }
        }
    }

    private fun createMissingWalletsIfNeeded(
        card: Card, session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val walletData = session.environment.walletData
        if (card.wallets.isEmpty()) {
            callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, walletData)))
            return
        }

        val curvesPresent = card.wallets.map { it.curve }
//        val curvesToCreate = EllipticCurve.values().subtract(curvesPresent)
        val curvesToCreate = card.supportedCurves.subtract(curvesPresent)

        if (curvesToCreate.isEmpty()) {
            callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, walletData)))
            return
        }

        CreateWalletsTask(curvesToCreate.toList()).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    callback(CompletionResult.Success(ScanResponse(result.data, ProductType.Other, walletData)))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }

    }

    private fun dealWithTwinCard(
        card: Card, session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.getSingleWallet()?.publicKey
                    if (publicKey == null) {
                        callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, null)))
                        return@run
                    }
                    val verified = TwinsHelper.verifyTwinPublicKey(
                        readDataResult.data.issuerData, publicKey
                    )
                    if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        callback(CompletionResult.Success(
                            ScanResponse(
                                card = card,
                                ProductType.Other,
                                walletData = session.environment.walletData,
                                secondTwinPublicKey = twinPublicKey.toHexString())
                        ))
                        return@run
                    } else {
                        callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, null)))
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, null)))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        if (card.isExcluded()) return TapSdkError.CardForDifferentApp
        // Disable new multi-currency HD wallet cards on the old version of the app
//        if (card.isMultiCurrencyWallet()) return UpdateAppToUseThisCard()
        return null
    }
}