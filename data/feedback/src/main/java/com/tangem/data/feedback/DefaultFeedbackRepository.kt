package com.tangem.data.feedback

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.core.navigation.email.EmailSender
import com.tangem.data.feedback.converters.BlockchainInfoConverter
import com.tangem.data.feedback.converters.WalletMetaInfoConverter
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.models.*
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Implementation of [FeedbackRepository]
 *
 * @property appLogsStore app logs store
 * @property userWalletsListRepository repository for getting user wallets
 * @property walletManagersStore wallet managers store
 * @property emailSender email sender
 * @property appInfoProvider app info provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultFeedbackRepository(
    private val appLogsStore: AppLogsStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletManagersStore: WalletManagersStore,
    private val emailSender: EmailSender,
    private val appInfoProvider: AppInfoProvider,
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

    override suspend fun getBlockchainInfo(userWalletId: UserWalletId, networkId: Network.ID): BlockchainInfo? {
        return walletManagersStore
            .getSyncOrNull(
                userWalletId = userWalletId,
                blockchain = networkId.toBlockchain(),
                derivationPath = networkId.derivationPath.value,
            )
            ?.let(BlockchainInfoConverter::convert)
    }

    override fun getPhoneInfo(): PhoneInfo {
        return PhoneInfo(
            phoneModel = appInfoProvider.device,
            osVersion = appInfoProvider.sdkVersion.toString(),
            appVersion = appInfoProvider.appVersion,
        )
    }

    override fun saveBlockchainErrorInfo(error: BlockchainErrorInfo) {
        val userWallet = userWalletsListRepository.selectedUserWallet.value ?: error("UserWallet is not selected")

        blockchainsErrors.update { map ->
            map.toMutableMap().apply {
                this[userWallet.walletId] = error
            }
        }
    }

    override fun getBlockchainErrorInfo(userWalletId: UserWalletId): BlockchainErrorInfo? {
        return blockchainsErrors.value[userWalletId].also {
            if (it == null) TangemLogger.e("Blockchain error info is null for $userWalletId")
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
        return userWalletsListRepository.userWalletsSync().find { it.walletId == userWalletId }
    }

    private fun totalUserWallets(): Int {
        return userWalletsListRepository.userWallets.value?.size ?: 0
    }
}