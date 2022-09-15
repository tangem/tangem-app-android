package com.tangem.tap.common

import android.content.Context
import com.tangem.tap.common.extensions.readAssetAsString
import com.tangem.tap.common.extensions.readFile
import com.tangem.tap.common.extensions.rewriteFile

/**
[REDACTED_AUTHOR]
 */
interface AssetReader {
    fun readAssetAsString(name: String): String
    fun readFile(fileName: String): String
    fun rewriteFile(content: String, fileName: String)
}

class AndroidAssetReader(
    private val context: Context,
) : AssetReader {

    override fun readAssetAsString(name: String): String {
        return context.readAssetAsString(name)
    }

    override fun readFile(fileName: String): String {
        return context.readFile(fileName)
    }

    override fun rewriteFile(content: String, fileName: String) {
        context.rewriteFile(content, fileName)
    }
}