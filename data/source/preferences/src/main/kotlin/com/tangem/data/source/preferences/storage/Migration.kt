package com.tangem.data.source.preferences.storage

internal interface Migration {
    fun migrate()
}