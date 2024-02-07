package com.tangem.lib.visa

import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import com.tangem.lib.visa.model.BalancesAndLimits
import com.tangem.lib.visa.utils.VisaConfig
import com.tangem.lib.visa.utils.VisaConfig.NETWORK_LOGS_TAG
import com.tangem.lib.visa.utils.toHexString
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import okhttp3.OkHttpClient
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.FastRawTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.StaticEIP1559GasProvider
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

interface VisaContractInfoProvider {

    suspend fun getBalancesAndLimits(walletAddress: String): BalancesAndLimits

    class Builder(
        private val isNetworkLoggingEnabled: Boolean,
        private val dispatchers: CoroutineDispatcherProvider,
        private val baseUrl: String = VisaConfig.BASE_RPC_URL,
        private val bridgeProcessorAddress: String = VisaConfig.BRIDGE_PROCESSOR_CONTRACT_ADDRESS,
        private val chainId: Long = VisaConfig.CHAIN_ID,
        private val networkTimeoutSeconds: Long = VisaConfig.NETWORK_TIMEOUT_SECONDS,
        private val decimals: Int = VisaConfig.DECIMALS,
        private val gasLimit: Long = VisaConfig.GAS_LIMIT,
        private val privateKey: String = ByteArray(VisaConfig.PRIVATE_KEY_LENGTH).toHexString(),
    ) {

        fun build(): VisaContractInfoProvider {
            val web3j = createWeb3J()
            val gasProvider = createGasProvider()
            val transactionManager = createTransactionManager(web3j)

            return DefaultVisaContractInfoProvider(
                web3j = web3j,
                transactionManager = transactionManager,
                gasProvider = gasProvider,
                bridgeProcessorAddress = bridgeProcessorAddress,
                dispatchers = dispatchers,
            )
        }

        private fun createWeb3J(): Web3j {
            val httpClient = OkHttpClient.Builder().apply {
                connectTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)
                readTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)
                writeTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)

                if (isNetworkLoggingEnabled) {
                    addInterceptor(
                        LoggingInterceptor.Builder()
                            .setLevel(Level.BODY)
                            .tag(NETWORK_LOGS_TAG)
                            .build(),
                    )
                }
            }.build()

            val web3jService = HttpService(
                /* url = */ baseUrl,
                /* httpClient = */ httpClient,
                /* includeRawResponses = */ false,
            )

            return Web3j.build(web3jService)
        }

        private fun createGasProvider(): ContractGasProvider = StaticEIP1559GasProvider(
            /* chainId = */ chainId,
            /* maxFeePerGas = */ BigDecimal.ONE.movePointLeft(decimals).toBigInteger(),
            /* maxPriorityFeePerGas = */ BigDecimal.ONE.movePointLeft(decimals).toBigInteger(),
            /* gasLimit = */ BigInteger.valueOf(gasLimit),
        )

        private fun createTransactionManager(web3j: Web3j): TransactionManager = FastRawTransactionManager(
            /* web3j = */ web3j,
            /* credentials = */ Credentials.create(privateKey),
            /* chainId = */ chainId,
        )
    }
}
