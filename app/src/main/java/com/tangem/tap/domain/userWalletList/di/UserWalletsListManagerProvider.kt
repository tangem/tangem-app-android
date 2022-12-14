package com.tangem.tap.domain.userWalletList.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.TangemSdkAdapter
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_sdk_new.storage.AndroidSecureStorage
import com.tangem.tangem_sdk_new.storage.createEncryptedSharedPreferences
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import com.tangem.tap.domain.userWalletList.implementation.BiometricUserWalletsListManager
import com.tangem.tap.domain.userWalletList.repository.implementation.BiometricUserWalletsKeysRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultSelectedUserWalletRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultUserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultUserWalletsSensitiveInformationRepository
import com.tangem.tap.domain.userWalletList.utils.json.ByteArrayKeyAdapter
import com.tangem.tap.domain.userWalletList.utils.json.CardBackupStatusAdapter
import com.tangem.tap.domain.userWalletList.utils.json.ExtendedPublicKeysMapAdapter
import com.tangem.tap.domain.userWalletList.utils.json.ScanResponseDerivedKeysMapAdapter
import com.tangem.tap.domain.userWalletList.utils.json.WalletDerivedKeysMapAdapter

private const val USER_WALLETS_STORAGE_NAME = "user_wallets_storage"

fun UserWalletsListManager.Companion.provideBiometricImplementation(
    context: Context,
    tangemSdkManager: TangemSdkManager,
): UserWalletsListManager {
    val moshi = Moshi.Builder()
        .add(WalletDerivedKeysMapAdapter())
        .add(ScanResponseDerivedKeysMapAdapter())
        .add(ByteArrayKeyAdapter())
        .add(ExtendedPublicKeysMapAdapter())
        .add(CardBackupStatusAdapter())
        .add(TangemSdkAdapter.DateAdapter())
        .add(TangemSdkAdapter.DerivationPathAdapter())
        .add(TangemSdkAdapter.DerivationNodeAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    val secureStorage = AndroidSecureStorage(
        preferences = SecureStorage.createEncryptedSharedPreferences(
            context = context,
            storageName = USER_WALLETS_STORAGE_NAME,
        ),
    )

    val keysRepository = BiometricUserWalletsKeysRepository(
        moshi = moshi,
        secureStorage = secureStorage,
        biometricManager = tangemSdkManager.biometricManager,
    )
    val publicInformationRepository = DefaultUserWalletsPublicInformationRepository(
        moshi = moshi,
        secureStorage = secureStorage,
    )
    val sensitiveInformationRepository = DefaultUserWalletsSensitiveInformationRepository(
        moshi = moshi,
        secureStorage = secureStorage,
    )
    val selectedUserWalletRepository = DefaultSelectedUserWalletRepository(
        secureStorage = secureStorage,
    )

    return BiometricUserWalletsListManager(
        keysRepository = keysRepository,
        publicInformationRepository = publicInformationRepository,
        sensitiveInformationRepository = sensitiveInformationRepository,
        selectedUserWalletRepository = selectedUserWalletRepository,
    )
}
