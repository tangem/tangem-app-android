package com.tangem.datasource.asset

/**
 * Asset file reader
 *
 * @author Anton Zhilenkov on 15/09/2022.
 */
interface AssetReader {

    /** Read content of json file [fileName] from asset */
    fun readJson(fileName: String): String
}
