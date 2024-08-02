package com.tangem.domain.feedback.repository

import com.tangem.domain.feedback.models.*
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWalletId
import java.io.File

interface FeedbackRepository {

    suspend fun getCardInfo(scanResponse: ScanResponse): CardInfo

    suspend fun getUserWalletsInfo(userWalletId: UserWalletId?): UserWalletsInfo

    suspend fun getBlockchainInfoList(userWalletId: UserWalletId): List<BlockchainInfo>

    fun getPhoneInfo(): PhoneInfo

    suspend fun getBlockchainInfo(
        userWalletId: UserWalletId,
        blockchainId: String,
        derivationPath: String?,
    ): BlockchainInfo?

    fun saveBlockchainErrorInfo(error: BlockchainErrorInfo)

    suspend fun getBlockchainErrorInfo(userWalletId: UserWalletId): BlockchainErrorInfo?

    suspend fun getAppLogs(): List<AppLogModel>

    suspend fun createLogFile(logs: String): File?
}
