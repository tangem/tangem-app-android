package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.GetAccountAddressesRequest
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.GetAccountAddressesResponse
import com.tangem.blockchain.extensions.Result as SdkResult
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.datasource.di.SdkMoshi
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcGetAddressesUseCase
import com.tangem.domain.walletconnect.usecase.method.WcNetworkDerivationState
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Use case for Bitcoin getAccountAddresses WalletConnect method.
 *
 * Returns wallet addresses filtered by intention (payment/ordinal).
 * This is a non-signing operation.
 */
@JsonClass(generateAdapter = true)
internal data class AddressInfo(
    @Json(name = "address") val address: String,
    @Json(name = "publicKey") val publicKey: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "intention") val intention: String? = null,
)

internal class WcBitcoinGetAccountAddressesUseCase @AssistedInject constructor(
    @Assisted val context: WcMethodUseCaseContext,
    @Assisted override val method: WcBitcoinMethod.GetAccountAddresses,
    private val walletManagersFacade: WalletManagersFacade,
    private val respondService: WcRespondService,
    @SdkMoshi private val moshi: Moshi,
) : WcGetAddressesUseCase {

    override val session: WcSession
        get() = context.session
    override val rawSdkRequest: WcSdkSessionRequest
        get() = context.rawSdkRequest
    override val network: Network
        get() = context.network
    override val derivationState: WcNetworkDerivationState = when {
        context.networkDerivationsCount > 1 -> WcNetworkDerivationState.Multiple(walletAddress = context.accountAddress)
        else -> WcNetworkDerivationState.Single
    }

    override suspend fun invoke(): Either<HandleMethodError, WcGetAddressesUseCase.GetAddressesResult> {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
        if (walletManager !is BitcoinWalletManager) {
            return HandleMethodError.UnknownError("Invalid wallet manager type").left()
        }

        return when (val result = walletManager.getAddresses(method.intentions)) {
            is SdkResult.Success -> {
                // Map SDK AddressInfo to WalletConnect AccountAddress
                val accountAddresses = result.data.map { addressInfo ->
                    com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.AccountAddress(
                        address = addressInfo.address,
                        publicKey = addressInfo.publicKey,
                        path = addressInfo.derivationPath,
                        intention = addressInfo.metadata?.get("intention") as? String,
                    )
                }

                val response = GetAccountAddressesResponse(addresses = accountAddresses)

                // Send response back to dApp
                val jsonResponse = buildJsonResponse(response)
                respondService.respond(rawSdkRequest, jsonResponse)

                // Return result for UI
                WcGetAddressesUseCase.GetAddressesResult(
                    addresses = accountAddresses.map { addr ->
                        WcGetAddressesUseCase.AddressInfo(
                            address = addr.address,
                            publicKey = addr.publicKey,
                            path = addr.path,
                            intention = addr.intention,
                        )
                    },
                ).right()
            }
            is SdkResult.Failure -> {
                HandleMethodError.UnknownError(result.error.customMessage).left()
            }
        }
    }

    override fun reject() {
        respondService.rejectRequestNonBlock(rawSdkRequest)
    }

    private fun buildJsonResponse(
        data: com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.GetAccountAddressesResponse,
    ): String {
        val addresses = data.addresses.map { addr ->
            AddressInfo(
                address = addr.address,
                publicKey = addr.publicKey,
                path = addr.path,
                intention = addr.intention,
            )
        }
        return moshi.adapter<List<AddressInfo>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, AddressInfo::class.java),
        ).toJson(addresses)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcBitcoinMethod.GetAccountAddresses,
        ): WcBitcoinGetAccountAddressesUseCase
    }
}