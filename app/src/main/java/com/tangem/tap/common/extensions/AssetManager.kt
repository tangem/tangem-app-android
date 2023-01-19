package com.tangem.tap.common.extensions

import android.content.res.AssetManager

fun AssetManager.readJsonFileToString(fileName: String): String =
    this.open("$fileName.json").bufferedReader().readText()
