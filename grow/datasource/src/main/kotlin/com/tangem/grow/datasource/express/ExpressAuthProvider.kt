package com.tangem.grow.datasource.express

interface ExpressAuthProvider {
    fun getSessionId(): String
}