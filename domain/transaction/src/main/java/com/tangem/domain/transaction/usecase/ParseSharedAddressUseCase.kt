package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.wallets.models.ParsedQrCode
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class ParseSharedAddressUseCase(
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(input: String, network: Network): Either<Throwable, ParsedQrCode> {
        return withContext(dispatchers.io) {
            Either.catch {
                walletAddressServiceRepository.parseSharedAddress(input, network)
            }
        }
    }
}