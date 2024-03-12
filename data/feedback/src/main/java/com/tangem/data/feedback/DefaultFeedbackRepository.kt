package com.tangem.data.feedback

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.tangem.data.feedback.converters.BlockchainInfoConverter
import com.tangem.data.feedback.converters.CardInfoConverter
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.models.AppLogModel
import com.tangem.domain.feedback.models.BlockchainInfo
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.PhoneInfo
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.wallets.models.UserWallet
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.StringWriter

/**
 * Implementation of [FeedbackRepository]
 *
 * @property appPreferencesStore application preferences store
 * @property userWalletsStore    user wallets store
 * @property walletManagersStore wallet managers store
 * @property context             context for getting app version
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultFeedbackRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val userWalletsStore: UserWalletsStore,
    private val walletManagersStore: WalletManagersStore,
    private val context: Context,
) : FeedbackRepository {

    override suspend fun getCardInfo(): CardInfo {
        return CardInfoConverter.convert(value = getSelectedUserWallet())
    }

    override suspend fun getBlockchainInfoList(): List<BlockchainInfo> {
        return walletManagersStore
            .getAllSync(userWalletId = getSelectedUserWallet().walletId)
            .map(BlockchainInfoConverter::convert)
    }

    override fun getPhoneInfo(): PhoneInfo {
        return PhoneInfo(
            phoneModel = Build.MODEL,
            osVersion = Build.VERSION.SDK_INT.toString(),
            appVersion = getAppVersion(),
        )
    }

    override suspend fun getAppLogs(): List<AppLogModel> {
        return appPreferencesStore.getObjectMap<String>(key = PreferencesKeys.APP_LOGS_KEY)
            .map { AppLogModel(timestamp = it.key.toLong(), message = it.value) }
    }

    override suspend fun createLogFile(logs: List<String>): File? {
        return try {
            val file = File(context.filesDir, LOGS_FILE)
            file.delete()
            file.createNewFile()

            val stringWriter = StringWriter()

            logs.forEach(stringWriter::append)

            val fileWriter = FileWriter(file)
            fileWriter.write(stringWriter.toString())
            fileWriter.close()

            file
        } catch (ex: Exception) {
            Timber.e(ex, "Logs file isn't created")
            null
        }
    }

    private fun getAppVersion(): String {
        return runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }
            .fold(
                onSuccess = PackageInfo::versionName,
                onFailure = {
                    Timber.e(it)
                    "x.y.z"
                },
            )
    }

    private fun getSelectedUserWallet(): UserWallet {
        return userWalletsStore.selectedUserWalletOrNull
            ?: error("UserWallet is not selected")
    }

    private companion object {
        const val LOGS_FILE = "logs.txt"
    }
}
