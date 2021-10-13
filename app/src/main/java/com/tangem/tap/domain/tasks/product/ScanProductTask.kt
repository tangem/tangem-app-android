package com.tangem.tap.domain.tasks.product

import com.tangem.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.WalletData
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.toHexString
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.ScanTask
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.TapWorkarounds.isTangemWallet
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.domain.twins.isTangemTwin

data class ScanResponse(
    val card: Card,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
) : CommandResponse {

    fun getBlockchain(): Blockchain {
        if (productType == ProductType.Note) return card.getTangemNoteBlockchain() ?: return Blockchain.Unknown
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

class ScanProductTask(val card: Card? = null) : CardSessionRunnable<ScanResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
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

private class ScanNoteProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        callback(CompletionResult.Success(ScanResponse(card, ProductType.Note, session.environment.walletData)))
    }
}

private class ScanWalletProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        callback(CompletionResult.Success(ScanResponse(card, ProductType.Wallet, session.environment.walletData)))
    }
}

private class ScanTwinProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.getSingleWallet()?.publicKey
                    if (publicKey == null) {
                        callback(CompletionResult.Success(ScanResponse(card, ProductType.Twin, null)))
                        return@run
                    }
                    val verified = TwinCardsManager.verifyTwinPublicKey(
                            readDataResult.data.issuerData, publicKey
                    )
                    if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        callback(CompletionResult.Success(
                                ScanResponse(
                                        card,
                                        ProductType.Twin,
                                        session.environment.walletData,
                                        twinPublicKey.toHexString())
                        ))
                        return@run
                    } else {
                        callback(CompletionResult.Success(ScanResponse(card, ProductType.Twin, null)))
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Success(ScanResponse(card, ProductType.Twin, null)))
            }
        }
    }

}

private class ScanOtherCardsProcessor : ProductCommandProcessor<ScanResponse> {

    override fun proceed(card: Card, session: CardSession, callback: (result: CompletionResult<ScanResponse>) -> Unit) {
        val walletData = session.environment.walletData

        if (card.wallets.isEmpty() || card.firmwareVersion < FirmwareVersion.MultiWalletAvailable) {
            callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, walletData)))
            return
        }

        val curvesToCreate = card.getCurvesForNonCreatedWallets()
        if (curvesToCreate.isEmpty()) {
            callback(CompletionResult.Success(ScanResponse(card, ProductType.Other, walletData)))
            return
        }

        CreateProductWalletsTask(curvesToCreate).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    PreflightReadTask(PreflightReadMode.FullCardRead, card.cardId).run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success -> {
                                val response = ScanResponse(readResult.data, ProductType.Other, walletData)
                                callback(CompletionResult.Success(response))
                            }
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}

fun Card.getCurvesForNonCreatedWallets(): List<EllipticCurve> {
    val curvesPresent = wallets.map { it.curve }
    val curvesForNonCreatedWallets = supportedCurves.subtract(curvesPresent)
    return curvesForNonCreatedWallets.toList()
}
