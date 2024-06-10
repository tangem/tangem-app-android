package com.tangem.domain.feedback.repository

import com.tangem.domain.feedback.models.*
import java.io.File

interface FeedbackRepository {

    suspend fun getUserWalletsInfo(): UserWalletsInfo

    suspend fun getCardInfo(): CardInfo

    suspend fun getBlockchainInfoList(): List<BlockchainInfo>

    suspend fun getBlockchainInfo(blockchainId: String, derivationPath: String?): BlockchainInfo?

    fun getPhoneInfo(): PhoneInfo

    fun saveBlockchainErrorInfo(error: BlockchainErrorInfo)

    suspend fun getBlockchainErrorInfo(): BlockchainErrorInfo?

    suspend fun getAppLogs(): List<AppLogModel>

    suspend fun createLogFile(logs: String): File?
}