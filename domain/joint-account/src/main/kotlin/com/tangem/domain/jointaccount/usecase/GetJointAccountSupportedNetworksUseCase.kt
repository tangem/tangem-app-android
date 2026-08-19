package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.models.network.Network

class GetJointAccountSupportedNetworksUseCase(
    private val repository: JointAccountSupportedNetworksRepository,
) {

    operator fun invoke(): List<Network.RawID> = repository.getSupportedNetworks()
}