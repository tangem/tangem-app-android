package com.tangem.tap.common

import android.content.Context
import com.tangem.tap.common.extensions.readFile
import com.tangem.tap.common.extensions.rewriteFile

interface FileReader {
    fun readFile(fileName: String): String
    fun rewriteFile(content: String, fileName: String)
}

class AndroidFileReader(private val context: Context) : FileReader {
    override fun readFile(fileName: String): String {
        return context.readFile(fileName)
    }

    override fun rewriteFile(content: String, fileName: String) {
        context.rewriteFile(content, fileName)
    }
}