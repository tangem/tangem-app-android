package com.tangem.google.drive.store

interface DriveAccountStore {

    suspend fun getAccountName(walletId: String): String?

    suspend fun setAccountName(walletId: String, accountName: String?)
}