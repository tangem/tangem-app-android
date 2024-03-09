package com.tangem.domain.feedback.repository

import com.tangem.domain.feedback.models.AppLogModel
import com.tangem.domain.feedback.models.BlockchainInfo
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.PhoneInfo
import java.io.File

interface FeedbackRepository {

    suspend fun getCardInfo(): CardInfo

    suspend fun getBlockchainInfoList(): List<BlockchainInfo>

    fun getPhoneInfo(): PhoneInfo

    suspend fun getAppLogs(): List<AppLogModel>

    suspend fun createLogFile(logs: List<String>): File?
}