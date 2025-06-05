package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.isAscii
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.network.ethereum.LegacySdkHelper.prepareToSendMessageData
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

@Suppress("LongParameterList")
internal class WcEthMessageSignUseCase @AssistedInject constructor(
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcEthMethod.MessageSign,
    private val walletManagersFacade: WalletManagersFacade,
    private val signUseCase: SignUseCase,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    override val securityStatus = blockAidDelegate.getSecurityStatus(
        network = network,
        method = method,
        rawSdkRequest = rawSdkRequest,
        session = session,
        accountAddress = context.accountAddress,
    )

    override suspend fun SignCollector<WcMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val hashToSign = LegacySdkHelper.createMessageData(method.rawMessage)
        val userWallet = session.wallet
        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: return

        val signedHash = signUseCase(hashToSign, userWallet, network)
            .onLeft { emit(state.toResult(it.left())) }
            .getOrNull() ?: return

        val respond = prepareToSendMessageData(signedHash, hashToSign, walletManager)

        val wcRespondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(wcRespondResult))
    }

    override fun invoke(): Flow<WcSignState<WcMessageSignUseCase.SignModel>> = flow {
        val model = WcMessageSignUseCase.SignModel(humanMsg = method.humanMsg)
        emitAll(delegate(model))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcEthMethod.MessageSign): WcEthMessageSignUseCase
    }
}

object LegacySdkHelper {
    private const val ETH_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"

    fun prepareToSendMessageData(signedHash: ByteArray, hashToSign: ByteArray, walletManager: WalletManager): String =
        UnmarshalHelper.unmarshalSignatureExtended(
            signature = signedHash,
            hash = hashToSign,
            publicKey = walletManager.wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM().toHexString().formatHex().lowercase() // use lowercase because some dapps cant handle UPPERCASE

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