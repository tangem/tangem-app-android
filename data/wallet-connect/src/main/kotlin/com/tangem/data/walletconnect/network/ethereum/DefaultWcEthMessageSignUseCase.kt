package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.isAscii
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class DefaultWcEthMessageSignUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted private val method: WcEthMethod.MessageSign,
    private val walletManagersFacade: WalletManagersFacade,
    private val signUseCase: SignUseCase,
) : BaseWcSignUseCase<Nothing, WcEthMessageSignUseCase.SignModel>(),
    WcEthMessageSignUseCase {

    override suspend fun SignCollector<WcEthMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcEthMessageSignUseCase.SignModel>,
    ) {
        val hashToSign = LegacySdkHelper.createMessageData(state.signModel.rawMsg)
        val userWallet = session.wallet
        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: return

        val signedHash = signUseCase(hashToSign, userWallet, network)
            .onLeft { emit(state.toResult(it.left())) }
            .getOrNull() ?: return

        val respond = EthereumUtils.prepareSignedMessageData(
            signedHash = signedHash,
            hashToSign = hashToSign,
            publicKey = walletManager.wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
        )

        val wcRespondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(wcRespondResult))
    }

    override fun invoke(): Flow<WcSignState<WcEthMessageSignUseCase.SignModel>> = flow {
        val model = WcEthMessageSignUseCase.SignModel(
            rawMsg = method.message,
            account = method.account,
            humanMsg = LegacySdkHelper.hexToAscii(method.message).orEmpty(),
        )
        emitAll(delegate(model))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.MessageSign): DefaultWcEthMessageSignUseCase
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