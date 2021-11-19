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
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toHexString
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.ScanTask
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.tap.domain.ProductType
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds
import com.tangem.tap.domain.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.twins.TwinsHelper

data class ScanResponse(
    val card: Card,
    val productType: ProductType,
    val walletData: WalletData?,
    val secondTwinPublicKey: String? = null,
) : CommandResponse {

    fun getBlockchain(): Blockchain {
        if (productType == ProductType.Note) return getTangemNoteBlockchain(card) ?: return Blockchain.Unknown
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

    fun isTangemNote(): Boolean = productType == ProductType.Note
    fun isTangemWallet(): Boolean = productType == ProductType.Wallet
    fun isTangemTwins(): Boolean = productType == ProductType.Twins
    fun isTangemOtherCards(): Boolean = productType == ProductType.Other

    fun twinsIsTwinned(): Boolean = card.isTangemTwins() && walletData != null && secondTwinPublicKey != null
}

class ScanProductTask(val card: Card? = null) : CardSessionRunnable<ScanResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val card = this.card ?: session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val error = getErrorIfExcludedCard(card)
        if (error != null) {
            callback(CompletionResult.Failure(error))
            return
        }

        val commandProcessor = when {
            TapWorkarounds.isTangemNote(card) -> ScanNoteProcessor()
            card.isTangemTwins() -> ScanTwinProcessor()
            TapWorkarounds.isTangemWallet(card) -> ScanWalletProcessor()
            else -> ScanOtherCardsProcessor()
        }
        commandProcessor.proceed(card, session) { processorResult ->
            when (processorResult) {
                is CompletionResult.Success -> ScanTask().run(session) { scanTaskResult ->
                    when (scanTaskResult) {
                        is CompletionResult.Success -> callback(CompletionResult.Success(processorResult.data))
                        is CompletionResult.Failure -> callback(CompletionResult.Failure(scanTaskResult.error))
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(processorResult.error))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        if (card.isExcluded()) return TapSdkError.CardForDifferentApp

        return null
    }
}

private fun Card.isTangemTwins(): Boolean = TwinsHelper.getTwinCardNumber(cardId) != null

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
                        callback(CompletionResult.Success(ScanResponse(card, ProductType.Twins, null)))
                        return@run
                    }
                    val verified = TwinsHelper.verifyTwinPublicKey(readDataResult.data.issuerData, publicKey)
                    if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        val walletData = session.environment.walletData
                        val response = ScanResponse(card, ProductType.Twins, walletData, twinPublicKey.toHexString())
                        callback(CompletionResult.Success(response))
                    } else {
                        callback(CompletionResult.Success(ScanResponse(card, ProductType.Twins, null)))
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Success(ScanResponse(card, ProductType.Twins, null)))
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