package com.tangem.data.transaction

import android.net.Uri
import androidx.core.text.isDigitsOnly
import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.ParsedQrCode
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.errors.ParsedQrCodeErrors
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DefaultWalletAddressServiceRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletAddressServiceRepository {

    override suspend fun validateAddress(userWalletId: UserWalletId, network: Network, address: String): Boolean =
        withContext(dispatchers.io) {
            val blockchain = Blockchain.fromId(network.rawId)

            if (blockchain.isNear()) {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = blockchain,
                    derivationPath = network.derivationPath.value,
                ) ?: return@withContext false
                (walletManager as? NearWalletManager)?.validateAddress(address) ?: false
            } else {
                blockchain.validateAddress(address)
            }
        }

    override fun validateMemo(network: Network, memo: String): Boolean {
        if (memo.isEmpty()) return true
        return when (network.rawId) {
            Blockchain.XRP.id -> {
                val tag = memo.toLongOrNull()
                tag != null && tag <= XRP_TAG_MAX_NUMBER
            }
            Blockchain.Stellar.id -> {
                isAssignableXlmValue(memo)
            }
            else -> true
        }
    }

    override suspend fun parseSharedAddress(input: String, network: Network): ParsedQrCode {
        val blockchain = Blockchain.fromId(network.rawId)
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

    private fun isAssignableXlmValue(value: String): Boolean {
        return when {
            value.isNotEmpty() && value.isDigitsOnly() -> {
                try {
                    // from com.tangem.blockchain.blockchains.stellar.StellarMemo.toStellarSdkMemo
                    value.toBigInteger() in BigInteger.ZERO..Long.MAX_VALUE.toBigInteger() * 2.toBigInteger()
                } catch (ex: NumberFormatException) {
                    false
                }
            }
            else -> {
                // from org.stellar.sdk.MemoText
                value.toByteArray().size <= XLM_MEMO_MAX_LENGTH
            }
        }
    }

    companion object {
        private const val XLM_MEMO_MAX_LENGTH = 28
        private const val XRP_TAG_MAX_NUMBER = 4294967295
    }
}