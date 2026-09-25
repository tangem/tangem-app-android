package com.tangem.data.walletconnect.network.ethereum

import arrow.core.left
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.formatHex
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
import com.tangem.domain.walletconnect.error.parseTangemSdkError
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
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

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
            .onLeft { emit(state.toResult(parseTangemSdkError(it).left())) }
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

    // TODO: [REDACTED_TASK_KEY] - Get this public function from Blockchain SDK
    fun prepareToSendMessageData(signedHash: ByteArray, hashToSign: ByteArray, walletManager: WalletManager): String =
        UnmarshalHelper.unmarshalSignatureExtended(
            signature = signedHash,
            hash = hashToSign,
            publicKey = walletManager.wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM().toHexString().formatHex().lowercase() // use lowercase because some dapps cant handle UPPERCASE

    fun createMessageData(message: String): ByteArray {
        val messageData = messageBytes(message)

        val prefixData = (ETH_MESSAGE_PREFIX + messageData.size.toString()).toByteArray()
        return (prefixData + messageData).toKeccak()
    }

    /**
     * The bytes `personal_sign` / `eth_sign` sign for [message], following the convention every major wallet
     * implements: a `0x`-prefixed hex string is the byte payload itself, anything else is the UTF-8 encoding of
     * the text. The previous implementation guessed — a plain-text message that happened to look like hex
     * ("deadbeef") was signed as bytes, and a non-ASCII text (Cyrillic, emoji) was signed as an EMPTY message.
     */
    fun messageBytes(message: String): ByteArray {
        return message.asHexBytesOrNull() ?: message.toByteArray(Charsets.UTF_8)
    }

    /**
     * Human-readable form of [message] for the confirmation sheet, or `null` when the payload is not text: a
     * `0x`-hex message is decoded as UTF-8 (previously only ASCII, so any non-ASCII byte blanked the "Contents"
     * row while the bytes were still signed), plain text is shown as is.
     */
    fun hexToAscii(message: String): String? {
        val bytes = message.asHexBytesOrNull() ?: return message
        val decoded = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull() ?: return null
        return decoded.takeIf { text -> text.none { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' } }
    }

    /** `0x` + an even number of hex digits → bytes; anything else (incl. odd length or non-hex) → `null`. */
    private fun String.asHexBytesOrNull(): ByteArray? {
        if (!startsWith(HEX_PREFIX, ignoreCase = true)) return null
        val digits = substring(HEX_PREFIX.length)
        if (digits.length % 2 != 0 || !digits.all { it.isAsciiHexDigit() }) return null
        return digits.hexToBytes()
    }

    private fun Char.isAsciiHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}