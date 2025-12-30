package com.tangem.domain.hotwallet

import com.tangem.domain.hotwallet.repository.HotWalletRepository

class IsHotWalletCreationSupported(private val hotWalletRepository: HotWalletRepository) {
    operator fun invoke(): Boolean = hotWalletRepository.isWalletCreationSupported()

    fun getLeastVersionName(): String = hotWalletRepository.getLeastSupportedAndroidVersionName()
}