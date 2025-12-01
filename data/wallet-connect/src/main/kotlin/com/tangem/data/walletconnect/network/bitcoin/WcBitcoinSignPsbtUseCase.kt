package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignPsbtRequest
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result as SdkResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.walletconnect.WcTransactionSignerProvider
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private const val WC_TAG = "WcBitcoinSignPsbt"

/**
 * Use case for Bitcoin signPsbt WalletConnect method.
 *
 * Signs a Partially Signed Bitcoin Transaction (BIP-174 PSBT) with optional broadcast.
 */
@Suppress("LongParameterList")
internal class WcBitcoinSignPsbtUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcBitcoinMethod.SignPsbt,
    private val walletManagersFacade: WalletManagersFacade,
    private val signerProvider: WcTransactionSignerProvider,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    blockAidDelegate: BlockAidVerificationDelegate,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase {

    override val securityStatus: LceFlow<Throwable, BlockAidTransactionCheck.Result> =
        blockAidDelegate.getSecurityStatus(
            network = network,
            method = method,
            rawSdkRequest = rawSdkRequest,
            session = session,
            accountAddress = context.accountAddress,
        ).map { lce ->
            lce.map { result -> BlockAidTransactionCheck.Result.Plain(result) }
        }

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        // Update wallet manager to refresh UTXO data before processing Bitcoin transaction
        Timber.tag(WC_TAG).i("Updating wallet manager...")
        walletManagersFacade.update(
            userWalletId = wallet.walletId,
            network = network,
            extraTokens = emptySet(),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
        Timber.tag(WC_TAG).i("Wallet manager type: ${walletManager?.javaClass?.simpleName}")

        if (walletManager !is BitcoinWalletManager) {
            Timber.tag("Wallet Connect").e("ERROR: Invalid wallet manager type")
            emit(state.toResult(HandleMethodError.UnknownError("Invalid wallet manager type").left()))
            return
        }

        Timber.tag(WC_TAG).i("Creating transaction signer...")
        val signer = createTransactionSigner()

        val request = SignPsbtRequest(
            psbt = method.psbt,
            signInputs = method.signInputs.map { input ->
                SignInput(
                    address = input.address,
                    index = input.index,
                    sighashTypes = input.sighashTypes,
                )
            },
            broadcast = method.broadcast,
        )
        when (val result = walletManager.walletConnectHandler.signPsbt(request, signer)) {
            is SdkResult.Success -> {
                Timber.tag(WC_TAG).i("✓ BTC SignPsbt SUCCESS")
                Timber.tag(WC_TAG).i("Response PSBT length: ${result.data.psbt.length}")
                Timber.tag(WC_TAG).d("Response PSBT first 100 chars: ${result.data.psbt.take(100)}")
                Timber.tag(WC_TAG).i("PSBT changed: ${result.data.psbt != method.psbt}")

                try {
                    val psbtBytes = java.util.Base64.getDecoder().decode(result.data.psbt)
                    Timber.tag(WC_TAG).i("PSBT decoded successfully, size: ${psbtBytes.size} bytes")
                } catch (e: Exception) {
                    Timber.tag("Wallet Connect").e("✗ Failed to decode PSBT as base64: ${e.message}")
                }

                result.data.txid?.let {
                    Timber.tag(WC_TAG).i("Transaction ID: $it")
                }

                val response = buildJsonResponse(result.data)
                val wcRespondResult = respondService.respond(rawSdkRequest, response)
                emit(state.toResult(wcRespondResult))
            }
            is SdkResult.Failure -> {
                emit(state.toResult(HandleMethodError.UnknownError(result.error.customMessage).left()))
            }
        }
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        // Create a compiled transaction data with PSBT bytes for display purposes
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.RawString(method.psbt),
        )
        return delegate.invoke(transactionData)
    }

    private fun createTransactionSigner() = signerProvider.createSigner(wallet)

    private fun buildJsonResponse(
        data: com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignPsbtResponse,
    ): String {
        return buildString {
            append("{")
            append("\"psbt\":\"${data.psbt}\"")
            data.txid?.let { append(",\"txid\":\"$it\"") }
            append("}")
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcBitcoinMethod.SignPsbt,
        ): WcBitcoinSignPsbtUseCase
    }
}