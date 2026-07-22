package com.tangem.features.foryou.impl.model.converter

import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/** Maps a [UserWallet] to the [ForYouWalletHeaderUM] shown as the header of a wallet group. */
internal class ForYouWalletHeaderConverter @Inject constructor(
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) : Converter<UserWallet, ForYouWalletHeaderUM> {

    override fun convert(value: UserWallet): ForYouWalletHeaderUM = ForYouWalletHeaderUM(
        id = value.walletId.stringValue,
        name = stringReference(value.name),
        deviceIcon = walletIconUMConverter.convert(getWalletIconUseCase(value)),
    )
}