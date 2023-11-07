package com.tangem.lib.auth

interface ExpressAuthProvider {

    fun getApiKey(): String

    fun getUserId(): String

    fun getSessionId(): String
}