package com.tangem.data.wallets

import androidx.core.text.isDigitsOnly
import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletAddressServiceRepository
import java.math.BigInteger

class DefaultWalletAddressServiceRepository(
    private val walletManagersFacade: WalletManagersFacade,
) : WalletAddressServiceRepository {

    override suspend fun validateAddress(userWalletId: UserWalletId, network: Network, address: String): Boolean {
        val blockchain = Blockchain.fromId(network.id.value)

        return if (blockchain.isNear()) {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = blockchain,
                derivationPath = network.derivationPath.value,
            ) ?: return false
            (walletManager as? NearWalletManager)?.validateAddress(address) ?: false
        } else {
            blockchain.validateAddress(address)
        }
    }

    override fun validateMemo(network: Network, memo: String): Boolean {
        if (memo.isEmpty()) return true
        return when (network.id.value) {
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