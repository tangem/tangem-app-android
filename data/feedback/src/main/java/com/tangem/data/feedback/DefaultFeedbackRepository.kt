package com.tangem.data.feedback

import android.os.Build
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.navigation.email.EmailSender
import com.tangem.data.feedback.converters.BlockchainInfoConverter
import com.tangem.data.feedback.converters.CardInfoConverter
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.models.*
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.version.AppVersionProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.io.File

/**
 * Implementation of [FeedbackRepository]
 *
 * @property appLogsStore           app logs store
 * @property userWalletsListManager user wallets list manager
 * @property walletManagersStore    wallet managers store
 * @property emailSender            email sender
 * @property appVersionProvider     app version provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultFeedbackRepository(
    private val appLogsStore: AppLogsStore,
    private val userWalletsListManager: UserWalletsListManager,
    private val walletManagersStore: WalletManagersStore,
    private val emailSender: EmailSender,
    private val appVersionProvider: AppVersionProvider,
) : FeedbackRepository {

    private val blockchainsErrors = MutableStateFlow<Map<UserWalletId, BlockchainErrorInfo>>(emptyMap())

    override fun getCardInfo(scanResponse: ScanResponse) = CardInfoConverter.convert(value = scanResponse)

    override fun getUserWalletsInfo(userWalletId: UserWalletId?): UserWalletsInfo {
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
            appVersion = appVersionProvider.versionName,
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

    override fun getBlockchainErrorInfo(userWalletId: UserWalletId): BlockchainErrorInfo? {
        return blockchainsErrors.value[userWalletId].also {
            if (it == null) Timber.e("Blockchain error info is null for $userWalletId")
        }
    }

    override fun getLogFile(): File? = appLogsStore.getFile()

    override suspend fun getZipLogFile(): File? = appLogsStore.getZipFile()

    override fun sendEmail(feedbackEmail: FeedbackEmail) {
        emailSender.send(
            EmailSender.Email(
                address = feedbackEmail.address,
                subject = feedbackEmail.subject,
                message = feedbackEmail.message,
                attachment = feedbackEmail.file,
            ),
        )
    }
}