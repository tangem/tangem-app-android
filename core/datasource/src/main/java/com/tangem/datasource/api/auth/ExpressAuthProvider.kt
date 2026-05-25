package com.tangem.datasource.api.auth

interface ExpressAuthProvider {
    fun getSessionId(): String
}