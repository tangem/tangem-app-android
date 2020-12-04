package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemError
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.commands.CommandResponse
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardStatus
import com.tangem.commands.common.card.masks.Product
import com.tangem.commands.file.ReadFileDataCommand
import com.tangem.commands.verifycard.VerifyCardCommand
import com.tangem.commands.verifycard.VerifyCardResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.tap.domain.TapSdkError
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tasks.ScanTask

data class ScanNoteResponse(
        val walletManager: WalletManager?,
        val card: Card,
        val verifyResponse: VerifyCardResponse? = null,
        val secondTwinPublicKey: String? = null
) : CommandResponse

class ScanNoteTask(val card: Card? = null) : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = false

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

                    if (card.cardData?.productMask?.contains(Product.TwinCard) == true) {
                        dealWithTwinCard(card, session, callback)
                        return@run
                    }

                    val walletManager = try {
                        WalletManagerFactory.makeWalletManager(card)
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
                            walletManager, card, verifyResult.data
                    )))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                }
            }
        }
    }

    private fun dealWithTwinCard(
            card: Card, session: CardSession,
            callback: (result: CompletionResult<ScanNoteResponse>) -> Unit
    ) {
        ReadFileDataCommand(readPrivateFiles = true).run(session) { filesResult ->
            when (filesResult) {
                is CompletionResult.Success -> {
                    val decoder = Tlv.deserialize(filesResult.data.fileData)?.let { TlvDecoder(it) }
                    val name = decoder?.decodeOptional<String>(TlvTag.FileName)
                    if (name != null && name == TwinsHelper.TWIN_FILE_NAME) {
                        val publicKey = decoder.decodeOptional<ByteArray>(TlvTag.FileData)?.toHexString()
                        val walletManager = try {
                            WalletManagerFactory.makeMultisigWalletManager(card, publicKey!!.hexToBytes())
                        } catch (exception: Exception) {
                            callback(CompletionResult.Success(ScanNoteResponse(null, card)))
                            return@run
                        }
                        verifyCard(walletManager, card, publicKey, session, callback)
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
        val productMask = card.cardData?.productMask ?: return TangemSdkError.CardError()
        if (!productMask.contains(Product.Note) && !productMask.contains(Product.TwinCard)) {
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