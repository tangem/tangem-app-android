package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserTokensResponseAddressesEnricher @Inject constructor(
    private val notificationsFeatureToggles: NotificationsFeatureToggles,
    private val walletsRepository: WalletsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val multiNetworkStatusProducerFactory: MultiNetworkStatusProducer.Factory,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        if (!notificationsFeatureToggles.isNotificationsEnabled) {
            return response
        }

        val isNotificationsEnabled = walletsRepository.isNotificationsEnabled(userWalletId)

        val multiNetworkStatusProducer = multiNetworkStatusProducerFactory.create(
            MultiNetworkStatusProducer.Params(userWalletId),
        )

        return withContext(dispatchers.io) {
            val networksStatus = multiNetworkStatusProducer.produce().first()

            val enrichedTokens = response.tokens.map { token ->
                if (isNotificationsEnabled) {
                    val matchingNetwork = networksStatus.find { status ->
                        status.network.backendId == token.networkId &&
                            status.network.derivationPath.value == token.derivationPath
                    } ?: return@map token

                    val addresses = (matchingNetwork.value as? NetworkStatus.Verified)
                        ?.address
                        ?.availableAddresses
                        ?.map { it.value }
                        ?.toList()
                        .orEmpty()

                    token.copy(list = addresses)
                } else {
                    token.copy(list = emptyList())
                }
            }

            response.copy(tokens = enrichedTokens, notifyStatus = isNotificationsEnabled)
        }
    }
}