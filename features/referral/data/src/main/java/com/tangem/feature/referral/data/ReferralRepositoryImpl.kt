package com.tangem.feature.referral.data

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.StartReferralBody
import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.referral.converters.ReferralConverter
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ReferralRepositoryImpl @Inject constructor(
    private val referralApi: TangemTechApi,
    private val referralConverter: ReferralConverter,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val demoModeDatasource: DemoModeDatasource,
    private val authProvider: AuthProvider,
    private val userWalletsStore: UserWalletsStore,
) : ReferralRepository {

    override val isDemoMode: Boolean
        get() = demoModeDatasource.isDemoModeActive

    override suspend fun getReferralData(walletId: String): ReferralData {
        return withContext(coroutineDispatcher.io) {
            referralConverter.convert(
                referralApi.getReferralStatus(
                    cardPublicKey = authProvider.getCardPublicKey(),
                    cardId = authProvider.getCardId(),
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
                    cardPublicKey = authProvider.getCardPublicKey(),
                    cardId = authProvider.getCardId(),
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
        val derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider

        val blockchain = Blockchain.fromNetworkId(tokenData.networkId)
            ?: error("Blockchain ${tokenData.networkId} not found")

        val cryptoCurrencyFactory = CryptoCurrencyFactory()

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
                derivationStyleProvider = derivationStyleProvider,
            )
        } else {
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                derivationStyleProvider = derivationStyleProvider,
            )
        }
    }
}