package com.tangem.feature.referral.data

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.StartReferralBody
import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.referral.converters.ReferralConverter
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ReferralRepositoryImpl @Inject constructor(
    private val referralApi: TangemTechApi,
    private val referralConverter: ReferralConverter,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val demoModeDatasource: DemoModeDatasource,
    private val userWalletsStore: UserWalletsStore,
    excludedBlockchains: ExcludedBlockchains,
) : ReferralRepository {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    override val isDemoMode: Boolean
        get() = demoModeDatasource.isDemoModeActive

    override suspend fun getReferralData(walletId: String): ReferralData {
        return withContext(coroutineDispatcher.io) {
            referralConverter.convert(
                referralApi.getReferralStatus(
                    walletId = walletId,
                ),
            )
        }
    }

    override suspend fun startReferral(
        walletId: String,
        networkId: String,
        tokenId: String,
        address: String,
    ): ReferralData {
        return withContext(coroutineDispatcher.io) {
            referralConverter.convert(
                referralApi.startReferral(
                    startReferralBody = StartReferralBody(
                        walletId = walletId,
                        networkId = networkId,
                        tokenId = tokenId,
                        address = address,
                    ),
                ),
            )
        }
    }

    override suspend fun getCryptoCurrency(userWalletId: UserWalletId, tokenData: TokenData): CryptoCurrency? {
        val userWallet = userWalletsStore.getSyncOrNull(userWalletId) ?: error("Wallet $userWalletId not found")

        val blockchain = Blockchain.fromNetworkId(tokenData.networkId)
            ?: error("Blockchain ${tokenData.networkId} not found")

        val contractAddress = tokenData.contractAddress
        val decimalCount = tokenData.decimalCount
        return if (contractAddress != null && decimalCount != null) {
            val sdkToken = Token(
                name = tokenData.name,
                symbol = tokenData.symbol,
                contractAddress = contractAddress,
                decimals = decimalCount,
                id = tokenData.id,
            )
            cryptoCurrencyFactory.createToken(
                sdkToken = sdkToken,
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = userWallet.scanResponse,
            )
        } else {
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = userWallet.scanResponse,
            )
        }
    }
}