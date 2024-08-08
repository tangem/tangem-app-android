package com.tangem.data.feedback

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.feedback.converters.BlockchainInfoConverter
import com.tangem.data.feedback.converters.CardInfoConverter
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.models.*
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.StringWriter

/**
 * Implementation of [FeedbackRepository]
 *
 * @property appPreferencesStore    application preferences store
 * @property userWalletsListManager user wallets list manager
 * @property walletManagersStore    wallet managers store
 * @property context                context for getting app version
 * @property dispatchers            coroutine dispatchers provider
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultFeedbackRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val userWalletsListManager: UserWalletsListManager,
    private val walletManagersStore: WalletManagersStore,
    private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : FeedbackRepository {

    private val blockchainsErrors = MutableStateFlow<Map<UserWalletId, BlockchainErrorInfo>>(emptyMap())

    override suspend fun getCardInfo(scanResponse: ScanResponse) = CardInfoConverter.convert(value = scanResponse)

    override suspend fun getUserWalletsInfo(userWalletId: UserWalletId?): UserWalletsInfo {
        return UserWalletsInfo(
            selectedUserWalletId = userWalletId?.stringValue ?: "card isn't activated",
            totalUserWallets = userWalletsListManager.walletsCount,
        )
    }

    override suspend fun getBlockchainInfoList(userWalletId: UserWalletId): List<BlockchainInfo> {
        return walletManagersStore
            .getAllSync(userWalletId = userWalletId)
            .map(BlockchainInfoConverter::convert)
    }

    override suspend fun getBlockchainInfo(
        userWalletId: UserWalletId,
        blockchainId: String,
        derivationPath: String?,
    ): BlockchainInfo? {
        return walletManagersStore
            .getSyncOrNull(
                userWalletId = userWalletId,
                blockchain = Blockchain.fromId(blockchainId),
                derivationPath = derivationPath,
            )
            ?.let(BlockchainInfoConverter::convert)
    }

    override fun getPhoneInfo(): PhoneInfo {
        return PhoneInfo(
            phoneModel = Build.MODEL,
            osVersion = Build.VERSION.SDK_INT.toString(),
            appVersion = getAppVersion(),
        )
    }

    override fun saveBlockchainErrorInfo(error: BlockchainErrorInfo) {
        val userWallet = userWalletsListManager.selectedUserWalletSync ?: error("UserWallet is not selected")

        blockchainsErrors.update {
            it.toMutableMap().apply {
                put(userWallet.walletId, error)
            }
        }
    }

    override suspend fun getBlockchainErrorInfo(userWalletId: UserWalletId): BlockchainErrorInfo? {
        return blockchainsErrors.value[userWalletId].also {
            if (it == null) Timber.e("Blockchain error info is null for $userWalletId")
        }
    }

    override suspend fun getAppLogs(): List<AppLogModel> {
        return appPreferencesStore.getObjectMapSync<String>(key = PreferencesKeys.APP_LOGS_KEY)
            .map { AppLogModel(timestamp = it.key.toLong(), message = it.value) }
            .sortedBy(AppLogModel::timestamp)
    }

    override suspend fun createLogFile(logs: String): File? {
        return runCatching(dispatchers.io) {
            val file = File(context.filesDir, LOGS_FILE)
            file.delete()
            file.createNewFile()

            val stringWriter = StringWriter()

            stringWriter.append(logs)

            val fileWriter = FileWriter(file)
            fileWriter.write(stringWriter.toString())
            fileWriter.close()

            file
        }.getOrElse {
            Timber.e(it, "Logs file isn't created")
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

    private companion object {
        const val LOGS_FILE = "logs.txt"
    }
}
