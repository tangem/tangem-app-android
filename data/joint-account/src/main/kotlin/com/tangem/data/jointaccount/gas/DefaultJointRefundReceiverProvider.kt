package com.tangem.data.jointaccount.gas

import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.jointaccount.gas.JointRefundReceiverProvider
import javax.inject.Inject

/**
 * Each backend environment accepts only its own treasury, so the address follows the environment the joint account
 * requests currently go to — it can be switched at runtime and doesn't depend on the build type.
 */
internal class DefaultJointRefundReceiverProvider @Inject constructor(
    private val apiConfigsManager: ApiConfigsManager,
) : JointRefundReceiverProvider {

    override suspend fun refundReceiver(): String {
        return when (apiConfigsManager.getEnvironmentConfig(TangemGatewayApiConfig.TANGEM_TECH_ID).environment) {
            ApiEnvironment.PROD,
            ApiEnvironment.PROD_NEW,
            -> PROD_TREASURY
            else -> TEST_TREASURY
        }
    }

    companion object {
        const val PROD_TREASURY = "0x2fD66Abd3BFeffA81851A9C61c10E979369A031c"
        const val TEST_TREASURY = "0x5F2C435DF8D00D0cF842a52Dd166B1Dcd69734C2"
    }
}