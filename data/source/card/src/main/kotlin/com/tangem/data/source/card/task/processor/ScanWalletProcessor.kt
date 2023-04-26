package com.tangem.data.source.card.task.processor

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.TangemSdkError
import com.tangem.common.deserialization.WalletDataDeserializer
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toMapKey
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.source.card.model.CardBlockchain
import com.tangem.data.source.card.model.ProductType
import com.tangem.data.source.card.model.ScanResult
import com.tangem.data.source.card.task.CreateWalletsTask
import com.tangem.data.source.card.utils.curvesForNonCreatedWallets
import com.tangem.data.source.card.utils.isFirmwareMultiwalletAllowed
import com.tangem.data.source.card.utils.isSaltPay
import com.tangem.data.source.card.utils.isStart2Coin
import com.tangem.data.source.card.utils.scope
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.backup.StartPrimaryCardLinkingTask
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.files.ReadFilesTask
import com.tangem.store.preferences.PreferencesStore
import kotlinx.coroutines.launch

internal class ScanWalletProcessor(
    private val blockchainsToDerive: Set<Blockchain>,
    private val preferencesStore: PreferencesStore,
) : ProductCommandProcessor<ScanResult> {

    private var primaryCard: PrimaryCard? = null
    override fun proceed(card: Card, session: CardSession, callback: (result: CompletionResult<ScanResult>) -> Unit) {
        @Suppress("MagicNumber")
        if (card.firmwareVersion.doubleValue >= 4.39 && card.settings.maxWalletsCount == 1) {
            readFile(card, session, callback)
            return
        }

        createMissingWalletsIfNeeded(card, session, callback)
    }

    private fun readFile(card: Card, session: CardSession, callback: (result: CompletionResult<ScanResult>) -> Unit) {
        val funToContinue = { createMissingWalletsIfNeeded(card, session, callback) }

        ReadFilesTask(fileName = "blockchainInfo", walletPublicKey = null).run(session) { result ->

            when (result) {
                is CompletionResult.Success -> {
                    val file = result.data.firstOrNull()
                    val counter = file?.counter
                    val signature = file?.signature

                    val tlv = file?.data?.let { Tlv.deserialize(it) }
                    val walletData = tlv?.let { WalletDataDeserializer.deserialize(TlvDecoder(tlv)) }

                    if (walletData == null || counter == null || signature == null) {
                        funToContinue()
                        return@run
                    }

                    val dataToVerify = card.cardId.hexToBytes() + file.data + counter.toByteArray()
                    val isVerified = CryptoUtils.verify(
                        publicKey = card.issuer.publicKey,
                        message = dataToVerify,
                        signature = signature,
                    )
                    if (!isVerified) {
                        funToContinue()
                        return@run
                    }

                    if (walletData.blockchain != "ANY") {
                        callback(
                            CompletionResult.Success(
                                ScanResult(
                                    card = card,
                                    productType = determineProductTypeForSingleCurrencyWallet(card),
                                    walletData = walletData,
                                ),
                            ),
                        )
                    } else {
                        funToContinue()
                    }
                }
                is CompletionResult.Failure -> {
                    when (result.error) {
                        is TangemSdkError.FileNotFound, is TangemSdkError.InsNotSupported -> {
                            funToContinue()
                        }
                        else -> callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
    }

    private fun determineProductTypeForSingleCurrencyWallet(card: Card): ProductType {
        return if (card.isStart2Coin) {
            ProductType.Start2Coin
        } else {
            ProductType.Note
        }
    }

    private fun createMissingWalletsIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResult>) -> Unit,
    ) {
        if (card.wallets.isNotEmpty() && card.backupStatus?.isActive == true) {
            startLinkingForBackupIfNeeded(card, session, callback)
            return
        }

        if (card.wallets.isEmpty() || !card.isFirmwareMultiwalletAllowed) {
            startLinkingForBackupIfNeeded(card, session, callback)
            return
        }

        val curvesToCreate = card.curvesForNonCreatedWallets
        if (curvesToCreate.isEmpty()) {
            startLinkingForBackupIfNeeded(card, session, callback)
            return
        }

        CreateWalletsTask(curvesToCreate).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    PreflightReadTask(
                        readMode = PreflightReadMode.FullCardRead,
                        cardId = card.cardId,
                    ).run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success -> startLinkingForBackupIfNeeded(card, session, callback)
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun startLinkingForBackupIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResult>) -> Unit,
    ) {
        val activationInProgress = preferencesStore.usedCardsPrefStorage.isActivationInProgress(card.cardId)

        @Suppress("ComplexCondition")
        if (card.backupStatus == Card.BackupStatus.NoBackup && card.wallets.isNotEmpty() &&
            (activationInProgress || card.isSaltPay)
        ) {
            StartPrimaryCardLinkingTask().run(session) { linkingResult ->
                when (linkingResult) {
                    is CompletionResult.Success -> {
                        primaryCard = linkingResult.data
                        deriveKeysIfNeeded(card, session, callback)
                    }
                    is CompletionResult.Failure -> {
                        deriveKeysIfNeeded(card, session, callback)
                    }
                }
            }
        } else {
            deriveKeysIfNeeded(card, session, callback)
        }
    }

    private fun deriveKeysIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResult>) -> Unit,
    ) {
        val productType = when (card.isSaltPay) {
            true -> ProductType.SaltPay
            else -> ProductType.Wallet
        }
        scope.launch {
            val derivations = collectDerivations(card)
            if (derivations.isEmpty() || !card.settings.isHDWalletAllowed) {
                callback(
                    CompletionResult.Success(
                        ScanResult(
                            card = card,
                            productType = productType,
                            walletData = session.environment.walletData,
                            primaryCard = primaryCard,
                        ),
                    ),
                )
                return@launch
            }

            DeriveMultipleWalletPublicKeysTask(derivations).run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        val response = ScanResult(
                            card = card,
                            productType = productType,
                            walletData = session.environment.walletData,
                            derivedKeys = result.data.entries,
                            primaryCard = primaryCard,
                        )
                        callback(CompletionResult.Success(response))
                    }
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun collectDerivations(card: Card): Map<ByteArrayKey, List<DerivationPath>> {
        val blockchains = blockchainsToDerive.map { CardBlockchain(it, card) }
        val derivations = mutableMapOf<ByteArrayKey, List<DerivationPath>>()

        blockchains.forEach { blockchain ->
            val curve = blockchain.primaryCurve
            val wallet = card.wallets.firstOrNull { it.curve == curve } ?: return@forEach
            if (wallet.chainCode == null) return@forEach

            val key = wallet.publicKey.toMapKey()
            val path = blockchain.derivationPath?.let { DerivationPath(it) }
            if (path != null) {
                val addedDerivations = derivations[key]
                if (addedDerivations != null) {
                    derivations[key] = addedDerivations + path
                } else {
                    derivations[key] = listOf(path)
                }
            }
        }
        return derivations
    }
}
