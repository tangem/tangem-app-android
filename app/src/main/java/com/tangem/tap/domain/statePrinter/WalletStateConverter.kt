package com.tangem.tap.domain.statePrinter

import com.tangem.blockchain.common.Wallet
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.redux.state.StringStateConverter
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class WalletStateConverter : StringStateConverter<AppState> {
    private val converter = MoshiJsonConverter.INSTANCE

    override fun convert(stateHolder: AppState): String {
        val walletState = stateHolder.walletState

        val walletManagers = mutableListOf<Map<String, Any?>>()
        walletState.walletManagers.forEach {
            val wallet = it.wallet

            val walletManagerMap = mutableMapOf<String, Any?>()
            walletManagerMap["walletManager"] = mapOf<String, Any>(
                "wallet" to convertWallet(it.wallet),
            )

            walletManagers.add(walletManagerMap)
        }
        val json = converter.prettyPrint(walletManagers)
        return json
    }

    private fun convertWallet(wallet: Wallet): MutableMap<String, Any> {
        val walletMap = mutableMapOf<String, Any>()
        val amounts = mutableMapOf<String, Any>()
        wallet.amounts.forEach { (type, amount) ->
            val amountMap = mapOf<String, Any?>(
                "value" to amount.value?.toPlainString(),
                "currencySymbol" to amount.currencySymbol,
                "decimals" to amount.decimals,
                "type" to amount.type::class.java.simpleName,
            )
            amounts[type::class.java.simpleName] = amountMap
        }

        val publicKeyMap = mapOf(
            "seedKey" to wallet.publicKey.seedKey,
            "derivedKey" to wallet.publicKey.derivedKey,
            "derivationPath" to wallet.publicKey.derivationPath?.rawPath,
            "blockchainKey" to wallet.publicKey.blockchainKey,
        )

        walletMap["address"] = wallet.address
        walletMap["blockchain"] = wallet.blockchain.name
        walletMap["curve"] = wallet.blockchain.getSupportedCurves()[0].curve
        walletMap["publicKey"] = publicKeyMap
        walletMap["amounts"] = amounts
        walletMap["addresses"] = wallet.addresses.toString()
        walletMap["cardId"] = wallet.cardId

        return walletMap
    }
}

fun printWalletState() {
    val stringState = WalletStateConverter().convert(store.state)
    Timber.d(stringState)
}