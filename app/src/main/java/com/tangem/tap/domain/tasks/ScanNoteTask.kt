package com.tangem.tap.domain.tasks

import com.tangem.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.toHexString
import com.tangem.operations.CommandResponse
import com.tangem.operations.ScanTask
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.domain.twins.isTangemTwin

data class ScanNoteResponse(
    val card: Card,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
) : CommandResponse {

    fun getBlockchain(): Blockchain {
        if (card.isTangemNote()) return card.getTangemNoteBlockchain() ?: return Blockchain.Unknown
        val blockchainName: String = walletData?.blockchain ?: return Blockchain.Unknown
        return Blockchain.fromId(blockchainName)
    }

    fun getPrimaryToken(): Token? {
        val cardToken = walletData?.token ?: return null
        return Token(
            cardToken.name,
            cardToken.symbol,
            cardToken.contractAddress,
            cardToken.decimals,
            Blockchain.fromId(walletData.blockchain)
        )
    }
}

class ScanNoteTask(val card: Card? = null) : CardSessionRunnable<ScanNoteResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit,
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

                    if (card.isTangemTwin()) {
                        dealWithTwinCard(card, session, callback)
                    } else if (!card.isTangemNote() && card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable) {
                        createMissingWalletsIfNeeded(card, session, callback)
                    } else {
                        callback(CompletionResult.Success(
                            ScanNoteResponse(card, session.environment.walletData)))
                    }
                }
            }
        }
    }

    private fun createMissingWalletsIfNeeded(
        card: Card, session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit,
    ) {
        val walletData = session.environment.walletData
        if (card.wallets.isEmpty()) {
            callback(CompletionResult.Success(ScanNoteResponse(card, walletData)))
            return
        }

        val curvesPresent = card.wallets.map { it.curve }
//        val curvesToCreate = EllipticCurve.values().subtract(curvesPresent)
        val curvesToCreate = card.supportedCurves.subtract(curvesPresent)

        if (curvesToCreate.isEmpty()) {
            callback(CompletionResult.Success(ScanNoteResponse(card, walletData)))
            return
        }

        CreateWalletsTask(curvesToCreate.toList()).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    callback(CompletionResult.Success(ScanNoteResponse(result.data, walletData)))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }

    }

    private fun dealWithTwinCard(
        card: Card, session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit,
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.getSingleWallet()?.publicKey
                    if (publicKey == null) {
                        callback(CompletionResult.Success(ScanNoteResponse(card, null)))
                        return@run
                    }
                    val verified = TwinCardsManager.verifyTwinPublicKey(
                        readDataResult.data.issuerData, publicKey
                    )
                    if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        callback(CompletionResult.Success(
                            ScanNoteResponse(
                                card = card,
                                walletData = session.environment.walletData,
                                secondTwinPublicKey = twinPublicKey.toHexString())
                        ))
                        return@run
                    } else {
                        callback(CompletionResult.Success(ScanNoteResponse(card, null)))
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Success(ScanNoteResponse(card, null)))
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