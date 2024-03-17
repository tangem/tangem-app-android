package com.tangem.domain.feedback.repository

import com.tangem.domain.feedback.models.*
import java.io.File

interface FeedbackRepository {

    suspend fun getUserWalletsInfo(): UserWalletsInfo

    suspend fun getCardInfo(): CardInfo

    suspend fun getBlockchainInfoList(): List<BlockchainInfo>

    fun getPhoneInfo(): PhoneInfo

    suspend fun getAppLogs(): List<AppLogModel>

    suspend fun createLogFile(logs: String): File?
}
