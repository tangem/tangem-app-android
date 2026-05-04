package com.tangem.data.transaction

import android.net.Uri
import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NameResolver
import com.tangem.blockchain.common.ResolveAddressResult
import com.tangem.blockchain.common.ReverseResolveAddressResult
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.MemoValidatorFacade
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.ParsedQrCode
import com.tangem.domain.wallets.models.errors.ParsedQrCodeErrors
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class DefaultWalletAddressServiceRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val memoValidatorFacade: MemoValidatorFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletAddressServiceRepository {

    override suspend fun getEns(userWalletId: UserWalletId, network: Network, address: String): String? {
        return withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()
            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = blockchain,
                derivationPath = network.derivationPath.value,
            )
            walletManager?.wallet?.ens.takeIf { it.isNullOrEmpty().not() }
        }
    }

    override suspend fun reverseResolveAddress(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
    ): ReverseResolveAddressResult {
        return withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()

            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = blockchain,
                derivationPath = network.derivationPath.value,
            )

            if (walletManager is NameResolver) {
                walletManager.reverseResolve(address)
            } else {
                ReverseResolveAddressResult.NotSupported
            }
        }
    }

    override suspend fun resolveAddress(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
    ): ResolveAddressResult {
        return withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()

            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = blockchain,
                derivationPath = network.derivationPath.value,
            )

            if (walletManager is NameResolver) {
                walletManager.resolve(address)
            } else {
                ResolveAddressResult.NotSupported
            }
        }
    }

    override suspend fun validateAddress(userWalletId: UserWalletId, network: Network, address: String): Boolean =
        withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()

            if (blockchain.isNear()) {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = blockchain,
                    derivationPath = network.derivationPath.value,
                ) ?: return@withContext false
                (walletManager as? NearWalletManager)?.validateAddress(address) == true
            } else {
                blockchain.validateAddress(address)
            }
        }

    override suspend fun validateMemo(network: Network, memo: String): Boolean =
        memoValidatorFacade.validateMemo(network, memo)

    override suspend fun isMemoRequired(network: Network, destinationAddress: String): Boolean =
        memoValidatorFacade.isMemoRequired(network, destinationAddress)

    override suspend fun parseSharedAddress(input: String, network: Network): ParsedQrCode {
        val blockchain = network.toBlockchain()
        val addressSchemeSplit = when (blockchain) {
            Blockchain.BitcoinCash, Blockchain.Kaspa -> listOf(input)
            else -> input.split(":")
        }

        val noSchemeAddress = when (addressSchemeSplit.size) {
            1 -> { // no scheme
                input
            }
            2 -> { // scheme
                if (blockchain.validateShareScheme(addressSchemeSplit[0])) {
                    addressSchemeSplit[1]
                } else {
                    // to preserve old logic
                    return ParsedQrCode(address = input)
                }
            }
            else -> { // invalid URI
                throw ParsedQrCodeErrors.InvalidUriError
            }
        }

        val uri = Uri.parse(noSchemeAddress)
        val address = uri.host ?: noSchemeAddress
        val amount = uri.getQueryParameter("amount")?.toBigDecimalOrNull()
        return ParsedQrCode(
            address = address,
            amount = amount,
        )
    }

    private fun Blockchain.isNear(): Boolean {
        return this == Blockchain.Near || this == Blockchain.NearTestnet
    }
}