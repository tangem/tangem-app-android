package com.tangem.lib.auth

interface ExpressAuthProvider {

    fun getUserId(): String

    fun getSessionId(): String

    fun getRefCode(): String
}