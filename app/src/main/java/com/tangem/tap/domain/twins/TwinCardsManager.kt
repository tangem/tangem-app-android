package com.tangem.tap.domain.twins

import com.tangem.Message
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.file.FileData
import com.tangem.commands.file.WriteFileDataCommand
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.tlv.TlvEncoder
import com.tangem.common.tlv.TlvTag
import com.tangem.common.tlv.serialize
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.tangemSdkManager

class TwinCardsManager(private val scanNoteResponse: ScanNoteResponse) {

    private val currentCardId: String = scanNoteResponse.card.cardId
    private val secondCardId: String? = TwinsHelper.getTwinsCardId(currentCardId)

    private var currentCardPublicKey: String? = null
    private var secondCardPublicKey: String? = null

    suspend fun createFirstWallet(message: Message): SimpleResult {
        val response = tangemSdkManager.runTaskAsync(
                CreateFirstTwinWalletTask(), currentCardId, message
        )
        when (response) {
            is CompletionResult.Success -> {
                currentCardPublicKey = response.data.walletPublicKey.toHexString()
                return SimpleResult.Success
            }
            is CompletionResult.Failure -> return SimpleResult.failure(response.error)
        }

    }


    suspend fun createSecondWallet(message: Message): SimpleResult {
        val response = tangemSdkManager.runTaskAsync(
                CreateSecondTwinWalletTask(currentCardPublicKey!!), secondCardId, message
        )
        when (response) {
            is CompletionResult.Success -> {
                secondCardPublicKey = response.data.walletPublicKey.toHexString()
                return SimpleResult.Success
            }
            is CompletionResult.Failure -> return SimpleResult.failure(response.error)
        }

    }

    suspend fun complete(message: Message): Result<ScanNoteResponse> {
        val response = tangemSdkManager.runTaskAsync(
                WriteFileDataCommand(createFileWithPublicKey(secondCardPublicKey!!)),
                currentCardId, message
        )
        return when (response) {
            is CompletionResult.Success -> {
                val walletManager = WalletManagerFactory.makeMultisigWalletManager(
                        scanNoteResponse.card, secondCardPublicKey!!.hexToBytes()
                )
                return Result.Success(scanNoteResponse.copy(
                        walletManager = walletManager,
                        secondTwinPublicKey = secondCardPublicKey
                ))
            }
            is CompletionResult.Failure -> Result.failure(response.error)
        }
    }

    companion object {
        fun createFileWithPublicKey(publicKey: String): FileData {
            val nameTlv = TlvEncoder().encode(TlvTag.FileName, "TwinPublicKey")
            val pubKey = TlvEncoder().encode(TlvTag.FileData, publicKey.hexToBytes())
            val tlvs = listOf(nameTlv, pubKey).serialize()
            return FileData.DataProtectedByPasscode(tlvs)
        }
    }
}