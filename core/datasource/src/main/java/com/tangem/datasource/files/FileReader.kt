package com.tangem.datasource.files

interface FileReader {

    fun readFile(fileName: String): String

    fun rewriteFile(content: String, fileName: String)

    fun removeFile(fileName: String)
}