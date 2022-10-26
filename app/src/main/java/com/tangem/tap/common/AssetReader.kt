package com.tangem.tap.common

import android.content.Context
import com.tangem.tap.common.extensions.readAssetAsString

/**
* [REDACTED_AUTHOR]
 */
interface AssetReader {
    fun readAssetAsString(name: String): String
}

class AndroidAssetReader(
    private val context: Context,
) : AssetReader {

    override fun readAssetAsString(name: String): String {
        return context.readAssetAsString(name)
    }
}
