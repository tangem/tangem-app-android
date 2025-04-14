package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.isAscii
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.OnSign
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.ethereum.WcPersonalEthSignUseCase
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletmanager.WalletManagersFacade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class DefaultWcPersonalEthSignUseCase(
    override val respondService: WcRespondService,
    override val context: WcMethodUseCaseContext,
    private val method: WcEthMethod.PersonalEthSign,
    private val walletManagersFacade: WalletManagersFacade,
    private val cardRepository: CardSdkConfigRepository,
) : BaseWcSignUseCase<Nothing, WcPersonalEthSignUseCase.SignModel>(),
    WcPersonalEthSignUseCase {

    override val onSign: OnSign<WcPersonalEthSignUseCase.SignModel> = collector@{ state ->
        val hashToSign = LegacySdkHelper.createMessageData(state.signModel.rawMsg)
        val userWallet = session.wallet
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins
        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: return@collector

        val signer = cardRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )

        val signResult = when (val signResult = signer.sign(hashToSign, walletManager.wallet.publicKey)) {
            is CompletionResult.Failure -> signResult.error.left()
            is CompletionResult.Success -> signResult.data.right()
        }

        val signedHash = signResult
            .onLeft { emit(state.toResult(it.left())) }
            .getOrNull() ?: return@collector

        val respond = EthereumUtils.prepareSignedMessageData(
            signedHash = signedHash,
            hashToSign = hashToSign,
            publicKey = walletManager.wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
        )

        val wcRespondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(wcRespondResult))
    }

    override fun invoke(): Flow<WcSignState<WcPersonalEthSignUseCase.SignModel>> = flow {
        val model = WcPersonalEthSignUseCase.SignModel(
            rawMsg = method.message,
            account = method.account,
            humanMsg = LegacySdkHelper.hexToAscii(method.message).orEmpty(),
        )
        emitAll(delegate(model))
    }
}

object LegacySdkHelper {
    private const val ETH_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"

    fun createMessageData(message: String): ByteArray {
        val messageData = try {
            message.removePrefix(HEX_PREFIX).hexToBytes()
        } catch (exception: Exception) {
            message.asciiToHex()?.hexToBytes() ?: byteArrayOf()
        }

        val prefixData = (ETH_MESSAGE_PREFIX + messageData.size.toString()).toByteArray()
        return (prefixData + messageData).toKeccak()
    }

    fun hexToAscii(hex: String): String? {
        return try {
            hex.removePrefix(HEX_PREFIX).hexToBytes().map {
                val char = it.toInt().toChar()
                if (char.isAscii()) char else return null
            }.joinToString("")
        } catch (exception: Exception) {
            return null
        }
    }

    private fun String.asciiToHex(): String? {
        return map {
            if (!it.isAscii()) return null
            Integer.toHexString(it.code)
        }.joinToString("")
    }
}