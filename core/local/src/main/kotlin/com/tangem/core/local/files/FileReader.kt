package com.tangem.core.local.files

interface FileReader {

    suspend fun readFile(fileName: String): String

    suspend fun rewriteFile(content: String, fileName: String)

    suspend fun removeFile(fileName: String)
}