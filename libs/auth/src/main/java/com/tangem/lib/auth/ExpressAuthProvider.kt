package com.tangem.lib.auth

interface ExpressAuthProvider {
    fun getSessionId(): String
}