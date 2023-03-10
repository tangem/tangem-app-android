package com.tangem.datasource.asset

/**
 * Asset file reader
 *
[REDACTED_AUTHOR]
 */
interface AssetReader {

    /** Read content of json file [fileName] from asset */
    fun readJson(fileName: String): String
}