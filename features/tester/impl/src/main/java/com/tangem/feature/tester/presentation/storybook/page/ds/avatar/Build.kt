package com.tangem.feature.tester.presentation.storybook.page.ds.avatar

import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.feature.tester.presentation.storybook.entity.TangemAvatarStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemAvatarStory>.build(): TangemAvatarStory {
    return TangemAvatarStory(
        variant = TangemAvatarStory.Variant.Account,
        accountVariant = TangemAvatarStory.AccountVariant.Crypto,
        fillColor = TangemAvatar.FillColor.AzureBlue,
        appearance = TangemAvatar.Appearance.Neutral,
        status = TangemAvatar.Status.Progress,
        transactionType = TangemAvatarStory.TransactionType.Send,
        address = TangemAvatarStory.AddressSample.First,
        hasAccountIndicator = false,
        hasLetter = false,
        isRemoteAsset = false,
        isRoundedSquare = true,
        onVariantChange = { variant ->
            updateStory { it.copy(variant = variant, isRoundedSquare = variant == TangemAvatarStory.Variant.Account) }
        },
        onAccountVariantChange = { accountVariant -> updateStory { it.copy(accountVariant = accountVariant) } },
        onFillColorChange = { color -> updateStory { it.copy(fillColor = color) } },
        onAppearanceChange = { appearance -> updateStory { it.copy(appearance = appearance) } },
        onStatusChange = { status -> updateStory { it.copy(status = status) } },
        onTransactionTypeChange = { type -> updateStory { it.copy(transactionType = type) } },
        onAddressChange = { address -> updateStory { it.copy(address = address) } },
        onAccountIndicatorToggle = { updateStory { it.copy(hasAccountIndicator = !it.hasAccountIndicator) } },
        onLetterToggle = { updateStory { it.copy(hasLetter = !it.hasLetter) } },
        onRemoteAssetToggle = { updateStory { it.copy(isRemoteAsset = !it.isRemoteAsset) } },
        onRoundedSquareToggle = { updateStory { it.copy(isRoundedSquare = !it.isRoundedSquare) } },
    )
}

internal val tangemAvatarStoryFactory
    get() = storyPageFactory(StateUpdater<TangemAvatarStory>::build)