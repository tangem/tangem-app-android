package com.tangem.datasource.asset

import java.io.InputStream

/**
 * Asset file reader
 *
* [REDACTED_AUTHOR]
 */
interface AssetReader {

    /** Read content of json file from asset
     * @param fileName - name of the file
     * */
    fun readJson(fileName: String): String

    /** Read content of a file [file] from asset as InputStream
     * @param file - name of the file with extension
     * */
    fun openFile(file: String): InputStream
}
