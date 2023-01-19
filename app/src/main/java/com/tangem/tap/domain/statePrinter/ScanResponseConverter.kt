package com.tangem.tap.domain.statePrinter

import com.tangem.common.extensions.toHexString
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.redux.state.StringStateConverter
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 27/04/2022.
 */
class ScanResponseConverter : StringStateConverter<AppState> {
    private val converter = MoshiJsonConverter.INSTANCE

    override fun convert(stateHolder: AppState): String {
        val scanResponse = stateHolder.globalState.scanResponse ?: return "NULL"

        val scanResponseMap = mutableMapOf<String, Any?>()
        val derivedKeysMap = mutableMapOf<String, Any?>()
        scanResponse.derivedKeys.forEach { (keyWalletPubKey, mapExPubKeys) ->
            val exPubKeysMap = mapExPubKeys.entries.map { (derivationPath, exPubKey) ->
                mapOf(
                    "derivationPath" to derivationPath.rawPath,
                    "extendedPublicKey" to mapOf(
                        "publicKey" to exPubKey.publicKey.toHexString(),
                        "chainCode" to exPubKey.chainCode.toHexString(),
                    ),
                )
            }
            derivedKeysMap[keyWalletPubKey.bytes.toHexString()] = exPubKeysMap
        }
        scanResponseMap["derivedKeys"] = derivedKeysMap

        val json = converter.prettyPrint(scanResponseMap)

        return json
    }
}

fun printScanResponseState() {
    val stringState = ScanResponseConverter().convert(store.state)
    Timber.d(stringState)
}