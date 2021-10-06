package com.tangem.tap.domain.tasks.product

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.toHexString
import com.tangem.operations.ScanTask
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.TapWorkarounds.isTangemWallet
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.tasks.CreateWalletsTask
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.domain.twins.isTangemTwin

class ScanProductTask(val card: Card? = null) : CardSessionRunnable<ScanNoteResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit,
    ) {
        ScanTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val card = this.card ?: result.data

                    val error = getErrorIfExcludedCard(card)
                    if (error != null) {
                        callback(CompletionResult.Failure(error))
                        return@run
                    }

                    val commandProcessor = when {
                        card.isTangemNote() -> ScanNoteProcessor()
                        card.isTangemTwin() -> ScanTwinProcessor()
                        card.isTangemWallet() -> ScanWalletProcessor()
                        else -> ScanOtherCardsProcessor()
                    }
                    commandProcessor.proceed(card, session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        if (card.isExcluded()) return TapSdkError.CardForDifferentApp

        return null
    }
}

private class ScanNoteProcessor : ProductCommandProcessor<ScanNoteResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {
        callback(CompletionResult.Success(ScanNoteResponse(card, session.environment.walletData)))
    }
}

private class ScanWalletProcessor : ProductCommandProcessor<ScanNoteResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {
        callback(CompletionResult.Success(ScanNoteResponse(card, session.environment.walletData)))
    }
}

private class ScanTwinProcessor : ProductCommandProcessor<ScanNoteResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
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

}

private class ScanOtherCardsProcessor : ProductCommandProcessor<ScanNoteResponse> {
    override fun proceed(card: Card, session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        val walletData = session.environment.walletData
        if (card.wallets.isEmpty()) {
            callback(CompletionResult.Success(ScanNoteResponse(card, walletData)))
            return
        }

        val curvesPresent = card.wallets.map { it.curve }
        val curvesToCreate = card.supportedCurves.subtract(curvesPresent)

        if (curvesToCreate.isEmpty()) {
            callback(CompletionResult.Success(ScanNoteResponse(card, walletData)))
            return
        }

        CreateWalletsTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    callback(CompletionResult.Success(ScanNoteResponse(result.data, walletData)))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}