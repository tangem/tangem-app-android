package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemError
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.CommandResponse
import com.tangem.commands.ReadIssuerDataCommand
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.commands.common.card.masks.Product
import com.tangem.commands.verifycard.VerifyCardCommand
import com.tangem.commands.verifycard.VerifyCardResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.TapWorkarounds
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.store
import com.tangem.tasks.ScanTask

data class ScanNoteResponse(
        val walletManager: WalletManager?,
        val card: Card,
        val verifyResponse: VerifyCardResponse? = null,
        val secondTwinPublicKey: String? = null,
        val multiwalletCard: Boolean = false,
) : CommandResponse

class ScanNoteTask(val card: Card? = null) : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

    private val blockchainSdkConfig = store.state.globalState.configManager?.config
            ?.blockchainSdkConfig ?: BlockchainSdkConfig()
    private val walletManagerFactory = WalletManagerFactory(blockchainSdkConfig)

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit) {
        ScanTask().run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))

                is CompletionResult.Success -> {
                    val card = this.card?.copy(
                            isPin1Default = result.data.isPin1Default,
                            isPin2Default = result.data.isPin2Default
                    ) ?: result.data

                    val error = getErrorIfExcludedCard(card)
                    if (error != null) {
                        callback(CompletionResult.Failure(error))
                        return@run
                    }

                    if (card.isTwinCard()) {
                        dealWithTwinCard(card, session, callback)
                        return@run
                    }

                    val walletManager = try {
                        walletManagerFactory.makeWalletManager(card)
                    } catch (exception: Exception) {
                        return@run callback(CompletionResult.Success(ScanNoteResponse(null, card)))
                    }
                    verifyCard(walletManager, card, null, session, callback)
                }
            }
        }
    }

    private fun verifyCard(
            walletManager: WalletManager?, card: Card, publicKey: String? = null,
            session: CardSession, callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {

        VerifyCardCommand(true).run(session) { verifyResult ->
            when (verifyResult) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(ScanNoteResponse(
                            walletManager, card, verifyResult.data, publicKey,
                            batchesAllowingMultiwallet.contains(card.cardData?.batchId)
                    )))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(TangemSdkError.CardVerificationFailed()))
                }
            }
        }
    }

    private fun dealWithTwinCard(
            card: Card, session: CardSession,
            callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val verified = TwinCardsManager.verifyTwinPublicKey(
                            readDataResult.data.issuerData, card.walletPublicKey
                    )
                    if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        val walletManager = try {
                            walletManagerFactory.makeMultisigWalletManager(card, twinPublicKey)
                        } catch (exception: Exception) {
                            callback(CompletionResult.Success(ScanNoteResponse(null, card)))
                            return@run
                        }
                        verifyCard(walletManager, card, twinPublicKey.toHexString(), session, callback)
                        return@run
                    } else {
                        callback(CompletionResult.Success(ScanNoteResponse(null, card)))
                    }
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Success(ScanNoteResponse(null, card)))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        val productMask = card.cardData?.productMask
        if (productMask != null &&  // product mask is on cards v2.30 and later
                !productMask.contains(Product.Note) && !productMask.contains(Product.TwinCard)) {
            return TapSdkError.CardForDifferentApp
        }
        if (excludedBatches.contains(card.cardData?.batchId)) {
            return TapSdkError.CardForDifferentApp
        }
        if (card.status == CardStatus.Purged) return TangemSdkError.CardIsPurged()
        if (card.status == CardStatus.NotPersonalized) return TangemSdkError.NotPersonalized()

        return null
    }

    companion object {
        private val excludedBatches = listOf("0027", "0030", "0031")
    }
}

val Card.isMultiwalletAllowed: Boolean
    get() {
        return cardData?.productMask?.contains(Product.TwinCard) != true
                && !TapWorkarounds.isStart2Coin
                && this.curve == EllipticCurve.Secp256k1
    }

private val batchesAllowingMultiwallet = listOf("FFFF")