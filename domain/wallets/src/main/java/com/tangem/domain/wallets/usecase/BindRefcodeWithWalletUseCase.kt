package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class BindRefcodeWithWalletUseCase @Inject constructor(
    private val walletsPromoRepository: WalletsPromoRepository,
) {

    private val mutex = Mutex()

    suspend operator fun invoke(refcode: String, campaign: String?): Either<Error, Unit> = either {
        ensure(refcode.isNotBlank()) { Error.InvalidRefcode }

        mutex.withLock {
            checkSavedRefcode()

            bindRefcode(refcode = refcode, campaign = campaign)

            saveConversionData(refcode = refcode, campaign = campaign)
        }
    }

    suspend fun retry(): Either<Error, Unit> = either {
        mutex.withLock {
            val conversionData = tryBindRefcodeAgain()

            saveConversionData(refcode = conversionData.refcode, campaign = conversionData.campaign)
        }
    }

    private suspend fun Raise<Error>.checkSavedRefcode() {
        walletsPromoRepository.getConversionData().onSome {
            raise(Error.RefcodeAlreadySaved)
        }
    }

    private suspend fun Raise<Error>.bindRefcode(refcode: String, campaign: String?) {
        catch(
            block = { walletsPromoRepository.bindRefcodeWithWallets(refcode, campaign) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private suspend fun Raise<Error>.tryBindRefcodeAgain(): AppsFlyerConversionData {
        return catch(
            block = { walletsPromoRepository.bindSavedRefcodeWithWallets() },
            catch = { raise(Error.DataError(it)) },
        )
    }

    private suspend fun Raise<Error>.saveConversionData(refcode: String, campaign: String?) {
        catch(
            block = { walletsPromoRepository.saveConversionData(refcode, campaign) },
            catch = { raise(Error.DataError(it)) },
        )
    }

    sealed interface Error {

        data object InvalidRefcode : Error

        data object RefcodeAlreadySaved : Error

        data class DataError(val throwable: Throwable) : Error
    }
}