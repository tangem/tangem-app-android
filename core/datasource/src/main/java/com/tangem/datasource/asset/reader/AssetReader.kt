package com.tangem.datasource.asset.reader

import java.io.InputStream

/**
 * Asset file reader
 *
* [REDACTED_AUTHOR]
 */
interface AssetReader {

    /** Read content of json file [fileName] from asset */
    fun readJson(fileName: String): String

    /** Open a file [file] from asset as InputStream */
    fun openFile(file: String): InputStream
}
