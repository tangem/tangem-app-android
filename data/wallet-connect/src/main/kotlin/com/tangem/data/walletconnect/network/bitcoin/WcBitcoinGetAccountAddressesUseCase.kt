package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.AccountAddress
import com.tangem.blockchain.common.address.AddressInfo
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
                val accountAddresses = mapToAccountAddresses(result.data)

                respondToApp(accountAddresses)

                buildUiResult(accountAddresses).right()
            }
            is SdkResult.Failure -> {
                HandleMethodError.UnknownError(result.error.customMessage).left()
            }
        }
    }

    private fun mapToAccountAddresses(addressInfoList: List<AddressInfo>): List<AccountAddress> {
        return addressInfoList.map { addressInfo ->
            AccountAddress(
                address = addressInfo.address,
                publicKey = addressInfo.publicKey,
                path = addressInfo.derivationPath,
                intention = addressInfo.metadata?.get("intention") as? String,
            )
        }
    }

    private suspend fun respondToApp(accountAddresses: List<AccountAddress>) {
        val jsonResponse = buildJsonResponse(accountAddresses)
        respondService.respond(rawSdkRequest, jsonResponse)
    }

    private fun buildUiResult(accountAddresses: List<AccountAddress>): WcGetAddressesUseCase.GetAddressesResult {
        return WcGetAddressesUseCase.GetAddressesResult(
            addresses = accountAddresses.map { addr ->
                WcGetAddressesUseCase.AddressInfo(
                    address = addr.address,
                    publicKey = addr.publicKey,
                    path = addr.path,
                    intention = addr.intention,
                )
            },
        )
    }

    override fun reject() {
        respondService.rejectRequestNonBlock(rawSdkRequest)
    }

    private fun buildJsonResponse(data: List<AccountAddress>): String {
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, AccountAddress::class.java)
        return moshi.adapter<List<AccountAddress>>(listType).toJson(data)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcBitcoinMethod.GetAccountAddresses,
        ): WcBitcoinGetAccountAddressesUseCase
    }
}