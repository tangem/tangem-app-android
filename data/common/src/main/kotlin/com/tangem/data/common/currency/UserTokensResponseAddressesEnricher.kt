package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
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
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        if (!notificationsFeatureToggles.isNotificationsEnabled) {
            return response
        }

        val isNotificationsEnabled = walletsRepository.isNotificationsEnabled(userWalletId)

        return withContext(dispatchers.default) {
            val networksStatuses = if (isNotificationsEnabled) {
                multiNetworkStatusSupplier.invoke(MultiNetworkStatusProducer.Params(userWalletId)).first()
            } else {
                emptySet()
            }

            val enrichedTokens = response.tokens.map { token ->
                if (isNotificationsEnabled) {
                    val matchingNetwork = networksStatuses.find { status ->
                        status.network.backendId == token.networkId &&
                            status.network.derivationPath.value == token.derivationPath
                    } ?: return@map token

                    val networkAddress = when (matchingNetwork.value) {
                        is NetworkStatus.Verified -> (matchingNetwork.value as NetworkStatus.Verified).address
                        is NetworkStatus.NoAccount -> (matchingNetwork.value as NetworkStatus.NoAccount).address
                        else -> null
                    }

                    val addresses = networkAddress
                        ?.availableAddresses
                        ?.map { it.value }
                        ?.toList()
                        .orEmpty()

                    token.copy(addresses = addresses)
                } else {
                    token.copy(addresses = emptyList())
                }
            }

            response.copy(tokens = enrichedTokens, notifyStatus = isNotificationsEnabled)
        }
    }
}