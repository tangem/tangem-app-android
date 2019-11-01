package com.tangem

interface DataStorage {

    fun getTerminalPublicKey(): ByteArray?
    fun getTerminalPrivateKey(): ByteArray?
    fun getPin1(): String?
    fun getPin2(): String?

}