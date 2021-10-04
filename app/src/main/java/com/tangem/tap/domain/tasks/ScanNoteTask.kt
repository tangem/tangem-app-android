package com.tangem.tap.domain.tasks

import com.tangem.*
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.CommandResponse
import com.tangem.commands.ReadIssuerDataCommand
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.commands.verification.VerifyCardCommand
import com.tangem.commands.verification.VerifyCardResponse
import com.tangem.commands.wallet.WalletConfig
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds.isExcluded
import com.tangem.tap.domain.TapWorkarounds.isMultiCurrencyWallet
import com.tangem.tap.domain.TapWorkarounds.isNote
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.extensions.getStatus
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.store
import com.tangem.tasks.PreflightReadCapable
import com.tangem.tasks.PreflightReadSettings
import com.tangem.tasks.ScanTask
import com.tangem.wallet.R

data class ScanNoteResponse(
        val card: Card,
        val verifyResponse: VerifyCardResponse? = null,
        val secondTwinPublicKey: String? = null,
) : CommandResponse

class ScanNoteTask(val card: Card? = null) : CardSessionRunnable<ScanNoteResponse>, PreflightReadCapable {
    override val requiresPin2 = false

    override fun preflightReadSettings() = PreflightReadSettings.FullCardRead

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        ScanTask().run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))

                is CompletionResult.Success -> {
                    val card = this.card?.copy(
                            isPin1Default = result.data.isPin1Default,
                            isPin2Default = result.data.isPin2Default,
                    ) ?: result.data

                    val error = getErrorIfExcludedCard(card)
                    if (error != null) {
                        callback(CompletionResult.Failure(error))
                        return@run
                    }

                    verifyCard(card, session, callback)
                }
            }
        }
    }

    private fun verifyCard(
            card: Card, session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {
        VerifyCardCommand(true).run(session) { verifyResult ->
            when (verifyResult) {
                is CompletionResult.Success -> {
                    if (card.isTwinCard()) {
                        dealWithTwinCard(card, session, verifyResult.data, callback)
                    } else if (card.firmwareVersion.major >= 4) {
                        createMissingWalletsIfNeeded(card, session, verifyResult.data, callback)
                    } else {
                        callback(CompletionResult.Success(ScanNoteResponse(card, verifyResult.data)))
                    }
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
                }
            }
        }
    }

    private fun createMissingWalletsIfNeeded(
            card: Card, session: CardSession, verifyResponse: VerifyCardResponse,
            callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {
        if (card.getStatus() == CardStatus.Empty) {
            callback(CompletionResult.Success(ScanNoteResponse(card, verifyResponse)))
            return
        }

        val curvesPresent = card.wallets.map { it.curve }
        val curvesToCreate = EllipticCurve.values().subtract(curvesPresent)

        if (curvesToCreate.isEmpty()) {
            callback(CompletionResult.Success(ScanNoteResponse(card, verifyResponse)))
            return
        }

        val configs = curvesToCreate.map { curve ->
            WalletConfig(
                    isReusable = null, prohibitPurgeWallet = null, curveId = curve,
                    signingMethods = null
            )
        }
        CreateWalletsTask(configs).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    callback(CompletionResult.Success(ScanNoteResponse(result.data, verifyResponse)))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }

    }

    private fun dealWithTwinCard(
            card: Card, session: CardSession, verifyResponse: VerifyCardResponse,
            callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
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
                                ScanNoteResponse(card, verifyResponse, twinPublicKey.toHexString())
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
        if (card.status == CardStatus.Purged) return TangemSdkError.WalletIsPurged()
        if (card.status == CardStatus.NotPersonalized) return TangemSdkError.NotPersonalized()
        // Disable new cards on the old version of the app // TODO: remove when cards are supported
        if (card.isMultiCurrencyWallet() || card.isNote()) return UpdateAppToUseThisCard()
        return null
    }

    private fun getWalletManagerFactory(): WalletManagerFactory {
        val blockchainSdkConfig = store.state.globalState.configManager?.config
                ?.blockchainSdkConfig ?: BlockchainSdkConfig()
        return WalletManagerFactory(blockchainSdkConfig)
    }

}

class UpdateAppToUseThisCard : TangemError {
    override val code: Int = 50005
    override var customMessage: String = code.toString()
    override val messageResId: Int = R.string.error_update_app
}


