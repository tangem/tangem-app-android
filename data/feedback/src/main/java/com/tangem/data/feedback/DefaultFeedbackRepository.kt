package com.tangem.data.feedback

import android.os.Build
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.navigation.email.EmailSender
import com.tangem.data.feedback.converters.BlockchainInfoConverter
import com.tangem.data.feedback.converters.WalletMetaInfoConverter
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.models.*
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
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
 * @property useNewUserWalletsRepository flag to use new user wallets repository
 * @property userWalletsListRepository   user wallets repository
 * @property walletManagersStore    wallet managers store
 * @property emailSender            email sender
 * @property appVersionProvider     app version provider
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultFeedbackRepository(
    private val appLogsStore: AppLogsStore,
    private val useNewUserWalletsRepository: Boolean,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val userWalletsListManager: UserWalletsListManager,
    private val walletManagersStore: WalletManagersStore,
    private val emailSender: EmailSender,
    private val appVersionProvider: AppVersionProvider,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
) : FeedbackRepository {

    private val blockchainsErrors = MutableStateFlow<Map<UserWalletId, BlockchainErrorInfo>>(emptyMap())

    override suspend fun getUserWalletMetaInfo(userWalletId: UserWalletId): WalletMetaInfo {
        val userWallet = getUserWalletById(userWalletId)
        return userWallet?.let {
            WalletMetaInfoConverter.convert(it)
        } ?: WalletMetaInfo(userWalletId)
    }

    override fun getUserWalletMetaInfo(scanResponse: ScanResponse): WalletMetaInfo {
        return WalletMetaInfoConverter.convert(value = scanResponse)
    }

    override fun getUserWalletsInfo(userWalletId: UserWalletId?): UserWalletsInfo {
        return UserWalletsInfo(
            selectedUserWalletId = userWalletId?.stringValue ?: "card isn't activated",
            totalUserWallets = totalUserWallets(),
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
        val userWallet = getSelectedWalletUseCase.sync().getOrNull() ?: error("UserWallet is not selected")

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

    private suspend fun getUserWalletById(userWalletId: UserWalletId): UserWallet? {
        return if (useNewUserWalletsRepository) {
            userWalletsListRepository.userWalletsSync().find { it.walletId == userWalletId }
        } else {
            userWalletsListManager.userWalletsSync.find { it.walletId == userWalletId }
        }
    }

    private fun totalUserWallets(): Int {
        return if (useNewUserWalletsRepository) {
            userWalletsListRepository.userWallets.value?.size ?: 0
        } else {
            userWalletsListManager.walletsCount
        }
    }
}