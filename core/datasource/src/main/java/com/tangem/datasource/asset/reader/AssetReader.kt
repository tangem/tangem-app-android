package com.tangem.datasource.asset.reader

/**
 * Asset file reader
 *
 * @see <a href = "https://www.notion.so/tangem/Assets-e045dd890413413faf34ce07ae47ff56">Documentation</a>
 *
[REDACTED_AUTHOR]
 */
interface AssetReader {

    /**
     * Read content of file from assets
     *
     * @param fullFileName name of file with extension. Example: file.json
     */
    suspend fun read(fullFileName: String): String
}