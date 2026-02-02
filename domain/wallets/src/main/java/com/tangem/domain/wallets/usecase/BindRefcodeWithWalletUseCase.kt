package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class BindRefcodeWithWalletUseCase @Inject constructor(
    private val walletsPromoRepository: WalletsPromoRepository,
) {

    private val mutex = Mutex()

    suspend operator fun invoke(conversionData: AppsFlyerConversionData): Either<Throwable, Unit> = either {
        mutex.withLock {
            catch(
                block = { walletsPromoRepository.bindRefcodeWithWallets(conversionData) },
                catch = ::raise,
            )
        }
    }

    suspend fun retry(): Either<Throwable, Unit> = either {
        mutex.withLock {
            catch(
                block = { walletsPromoRepository.retryBindRefcodeWithWallets() },
                catch = ::raise,
            )
        }
    }
}