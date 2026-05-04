# Navigation Graph

Complete navigation map of the app based on `AppRoute` sealed class and feature-internal routes.

## 1. All AppRoute Paths

58 top-level routes defined in `common/routing/src/main/kotlin/com/tangem/common/routing/AppRoute.kt`.

| # | Route | Path | Description |
|---|-------|------|-------------|
| 1 | `Initial` | `/initial` | App entry point (splash) |
| 2 | `Home` | `/home` | Stories/home screen with launch mode |
| 3 | `Welcome` | `/welcome` | Welcome screen for returning users |
| 4 | `Disclaimer` | `/disclaimer` | Terms of service / disclaimer |
| 5 | `Wallet` | `/wallet` | Main wallet portfolio screen |
| 6 | `CurrencyDetails` | `/currency_details/{walletId}/{currencyId}` | Token/coin detail screen |
| 7 | `Send` | `/send/{walletId}/{currencyId}` | Send cryptocurrency |
| 8 | `Details` | `/details/{walletId}` | Wallet details / settings hub |
| 9 | `DetailsSecurity` | `/details/security` | Security mode settings |
| 10 | `Usedesk` | `/usedesk/{walletId}` | Customer support (Usedesk) |
| 11 | `CardSettings` | `/card_settings/{walletId}` | Card-specific settings |
| 12 | `AppSettings` | `/app_settings` | Global app settings |
| 13 | `ResetToFactory` | `/reset_to_factory/{walletId}/{cardId}/...` | Factory reset flow |
| 14 | `AccessCodeRecovery` | `/access_code_recovery` | Access code recovery |
| 15 | `ManageTokens` | `{source}/manage_tokens/{accountId}` | Add/remove tokens in portfolio |
| 16 | `ChooseManagedTokens` | `/{source}/choose_managed_tokens/...` | Token chooser for send-via-swap |
| 17 | `WalletConnectSessions` | `/wallet_connect_sessions` | WalletConnect sessions list |
| 18 | `QrScanning` | `/{source}/qr_scanning` | QR code scanner |
| 19 | `ReferralProgram` | `/referral_program` | Referral program |
| 20 | `Swap` | `/swap/{fromId}/{toId}/{walletId}/...` | Token swap screen |
| 21 | `AppCurrencySelector` | `/app_currency_selector` | Fiat currency selector |
| 22 | `Staking` | `/staking/{walletId}/{currencyId}/{integrationId}` | Staking screen |
| 23 | `PushNotification` | `/push_notification` | Push notification opt-in |
| 24 | `WalletSettings` | `/wallet_settings/{walletId}` | Per-wallet settings |
| 25 | `WalletBackup` | `/wallet_backup/{walletId}/{coldOption}` | Wallet backup options |
| 26 | `WalletHardwareBackup` | `/wallet_hardware_backup/{walletId}` | Hardware wallet backup |
| 27 | `Markets` | `/markets` | Markets token list |
| 28 | `MarketsTokenDetails` | `/markets_token_details/{tokenId}/{showPortfolio}` | Market token detail |
| 29 | `Onramp` | `/onramp/{walletId}/{symbol}` | Buy crypto (onramp) |
| 30 | `OnrampSuccess` | `/onramp/success/{txId}` | Onramp success screen |
| 31 | `BuyCrypto` | `/buy_crypto/{walletId}` | Buy crypto token selector |
| 32 | `SellCrypto` | `/sell_crypto/{walletId}` | Sell crypto token selector |
| 33 | `SwapCrypto` | `/swap_crypto/{walletId}` | Swap crypto token selector |
| 34 | `Onboarding` | `/onboarding_v2/{mode}` | Onboarding flow (v2) |
| 35 | `Stories` | `/stories$storyId` | Stories / promotional content |
| 36 | `NFT` | `/nft/{walletId}` | NFT collection list |
| 37 | `NFTSend` | `/send/nft/{walletId}/{collection}/{assetId}` | Send NFT |
| 38 | `CreateWalletSelection` | `/create_wallet_selection` | Choose wallet creation type |
| 39 | `CreateWalletStart` | `/create_wallet_start` | Wallet creation intro (cold/hot) |
| 40 | `CreateHardwareWallet` | `/create_hardware_wallet` | Create hardware wallet flow |
| 41 | `CreateMobileWallet` | `/create_mobile_wallet` | Create mobile (hot) wallet |
| 42 | `UpgradeWallet` | `/upgrade_wallet/{walletId}` | Upgrade hot wallet to hardware |
| 43 | `AddExistingWallet` | `/add_existing_wallet` | Import existing wallet |
| 44 | `WalletActivation` | `/wallet_activation/{walletId}` | Activate wallet post-creation |
| 45 | `CreateWalletBackup` | `/create_wallet_backup/{walletId}` | Backup flow for created wallet |
| 46 | `UpdateAccessCode` | `/update_access_code/{walletId}` | Change access code |
| 47 | `ViewPhrase` | `/view_seed_phrase/{walletId}` | View recovery phrase |
| 48 | `ForgetWallet` | `/forget_wallet/{walletId}` | Remove wallet from app |
| 49 | `SendEntryPoint` | `/send_entry_point/{walletId}/{currencyId}` | Send entry with swap option |
| 50 | `CreateAccount` | `/create_account/{walletId}` | Create new account |
| 51 | `EditAccount` | `/edit_account/{accountId}` | Edit account |
| 52 | `AccountDetails` | `/account_details/{accountId}` | Account details screen |
| 53 | `ArchivedAccountList` | `/archived_account/{walletId}` | Archived accounts list |
| 54 | `TangemPayDetails` | `/tangem_pay_details/{walletId}` | Tangem Pay card details |
| 55 | `TangemPayOnboarding` | `/tangem_pay_onboarding/{mode}` | Tangem Pay onboarding |
| 56 | `Kyc` | `/kyc` | KYC verification |
| 57 | `YieldSupplyEntry` | `/yield_supply_entry/{walletId}/{symbol}` | Yield/supply entry point |
| 58 | `NewsDetails` | `/news_details/{newsId}` | News article detail |

## 2. Navigation Edges

Each entry shows: **Source route** → target routes it can navigate to (via `push` or `replaceAll`).

### Initial / Bootstrap

| Source | Target | Method | Trigger |
|--------|--------|--------|---------|
| `Initial` | `Home`, `Welcome`, `Disclaimer`, `Onboarding`, etc. | `replaceAll` | App startup (DefaultRoutingComponent) |

### Home

| Target | Method | Trigger |
|--------|--------|---------|
| `ManageTokens(STORIES)` | push | After scan, manage tokens |
| `CreateWalletStart` | push | Create wallet from home |
| `Wallet` | replaceAll | After wallet saved / already saved |

### Welcome

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateWalletSelection` | push | "Add new wallet" button |
| `Home()` | replaceAll | When wallets list becomes empty |
| `Wallet` | replaceAll | After scan / wallet unlock / biometric |

### Disclaimer

| Target | Method | Trigger |
|--------|--------|---------|
| `PushNotification(Stories)` | push | After accepting TOS (stories flow) |
| `Home()` | replaceAll | After accepting TOS (non-stories flow) |

### Wallet (main portfolio)

| Target | Method | Trigger |
|--------|--------|---------|
| `Details` | push | Open wallet details |
| `ManageTokens(ACCOUNT)` | push | Manage tokens for account |
| `Onboarding` | push | Continue backup / onboarding |
| `CurrencyDetails` | push | Tap on a token |
| `Home` | push | Open stories |
| `NFT` | push | Open NFT collection |
| `TangemPayOnboarding` | push | Tangem Pay banner |
| `TangemPayDetails` | push | Tangem Pay card details |
| `YieldSupplyEntry` | push | Yield supply action |
| `QrScanning(MainScreen)` | push | QR scanner |
| `Send` | push | Send from QR / action |
| `WalletBackup` | push | Backup warning banner |

### CurrencyDetails (token details)

| Target | Method | Trigger |
|--------|--------|---------|
| `Onramp` | push | Buy action |
| `SendEntryPoint` | push | Send action |
| `Swap` | push | Swap action |
| `CurrencyDetails` | push | Navigate to related token (from staking router) |
| `Staking` | push | Open staking (from token details router) |

### Details (wallet details hub)

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateWalletSelection` | push | Add new wallet |
| `WalletSettings` | push | Open wallet settings |
| `WalletConnectSessions` | push | WalletConnect item |
| `AppSettings` | push | App settings item |
| `Disclaimer(isTosAccepted=true)` | push | View TOS |
| `Usedesk` | push | Customer support |
| `TangemPayOnboarding(FromBannerInSettings)` | push | Tangem Pay banner |

### WalletSettings

| Target | Method | Trigger |
|--------|--------|---------|
| `ReferralProgram` | push | Referral program |
| `WalletHardwareBackup` | push | Hardware backup |
| `CardSettings` | push | Card settings |
| `ForgetWallet` | push | Delete/forget wallet |
| `ViewPhrase` | push | View seed phrase |
| `AccountDetails` | push | Open account details |
| `ArchivedAccountList` | push | View archived accounts |
| `CreateAccount` | push | Create new account |
| `Home()` | replaceAll | After wallet deletion completes |

### WalletBackup

| Target | Method | Trigger |
|--------|--------|---------|
| `WalletActivation` | push | Start activation (no backup) |
| `ViewPhrase` | push | View phrase option |
| `WalletHardwareBackup` | push | Hardware backup option |

### WalletHardwareBackup

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateHardwareWallet` | push | Create new hardware wallet |
| `UpgradeWallet` | push | Upgrade current hot wallet |
| `CreateWalletBackup` | push | Backup existing wallet |

### CreateWalletSelection

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateMobileWallet` | push | Choose mobile wallet |
| `CreateHardwareWallet` | push | Choose hardware wallet |

### CreateWalletStart

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateMobileWallet` | push | Create mobile wallet |
| `Wallet` | replaceAll | After wallet creation completes |

### CreateMobileWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `AddExistingWallet` | push | Import existing wallet |
| `Wallet` | replaceAll | After creation completes |

### CreateHardwareWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Wallet` | replaceAll | After hardware wallet created |

### AddExistingWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Wallet` | replaceAll | After import completes |

### UpgradeWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Onboarding(UpgradeHotWallet)` | push | Start upgrade onboarding |

### CreateWalletBackup

| Target | Method | Trigger |
|--------|--------|---------|
| `UpgradeWallet` | push | After backup, continue to upgrade |

### AccountDetails

| Target | Method | Trigger |
|--------|--------|---------|
| `EditAccount` | push | Edit account |

### ForgetWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Home()` | replaceAll | After wallet forgotten |

### WalletConnectSessions

| Target | Method | Trigger |
|--------|--------|---------|
| `QrScanning(WalletConnect)` | push | Scan WC QR code |

### Onboarding

| Target | Method | Trigger |
|--------|--------|---------|
| `Home()` | replaceAll | Onboarding completed (no wallets) |
| `Wallet` | replaceAll | Onboarding completed (has wallets) |

### PushNotification

| Target | Method | Trigger |
|--------|--------|---------|
| `Home()` | replaceAll | After push notification opt-in (via nextRoute param) |

### Staking

| Target | Method | Trigger |
|--------|--------|---------|
| `CurrencyDetails` | push | Back to token details |

### TangemPayDetails

| Target | Method | Trigger |
|--------|--------|---------|
| `Swap` | push | Top up / withdraw via swap |

### NFT

| Target | Method | Trigger |
|--------|--------|---------|
| `NFTSend` | push | Send NFT |

### Send (notifications)

| Target | Method | Trigger |
|--------|--------|---------|
| `CurrencyDetails` | push | Navigate to fee token |

### SwapCrypto / BuyCrypto / SellCrypto

| Target | Method | Trigger |
|--------|--------|---------|
| `Swap` | push | After token selection (SwapCrypto) |
| `Onramp` | push | After token selection (BuyCrypto/SellCrypto) |

### Deep Link Handlers (push to AppRoute)

| Handler | Target Route |
|---------|-------------|
| `OnrampDeepLinkHandler` | Processes onramp callback params |
| `SellRedirectDeepLinkHandler` | `Send` (with sell redirect params) |
| `BuyDeepLinkHandler` | `BuyCrypto` |
| `SellDeepLinkHandler` | `SellCrypto` |
| `SwapDeepLinkHandler` | `SwapCrypto` |
| `ReferralDeepLinkHandler` | Referral handling |
| `WalletDeepLinkHandler` | Wallet handling |
| `TokenDetailsDeepLinkHandler` | `CurrencyDetails` |
| `StakingDeepLinkHandler` | `Staking` |
| `MarketsDeepLinkHandler` | `Markets` |
| `MarketsTokenDetailDeepLinkHandler` | `MarketsTokenDetails` |
| `WalletConnectDeepLinkHandler` | WalletConnect pairing |
| `PromoDeeplinkHandler` | Promo handling |
| `OnboardVisaDeepLinkHandler` | `TangemPayOnboarding` |
| `NewsDetailsDeepLinkHandler` | `NewsDetails` |

## 3. Nested Routes (Feature-Internal Navigation)

### OnboardingRoute
**File:** `features/onboarding-v2/impl/.../routing/OnboardingRoute.kt`

| Route | Description |
|-------|-------------|
| `None` | Initial empty state |
| `Note` | Single-card onboarding note |
| `MultiWallet` | Multi-wallet onboarding (with seed phrase flow option) |
| `Visa` | Visa card onboarding |
| `Twins` | Twin cards onboarding |
| `ManageTokens` | Token management during onboarding |
| `AskBiometry` | Biometry setup prompt |
| `Done` | Onboarding completion |

### WalletRoute
**File:** `features/wallet/impl/.../navigation/WalletRoute.kt`

| Route | Description |
|-------|-------------|
| `Wallet` | Main wallet view |
| `OrganizeTokens` | Reorder tokens in portfolio |

### SendEntryRoute
**File:** `features/send-v2/api/.../entry/SendEntryRoute.kt`

| Route | Description |
|-------|-------------|
| `Send` | Direct send flow |
| `SendWithSwap` | Send with swap option |
| `ChooseToken` | Token chooser for send-via-swap |

### CommonSendRoute (Send internal)
Used internally by `SendModel` and `NFTSendModel`:
- `Amount` → `Destination` → `Confirm` → `ConfirmSuccess`
- Edit mode: `Confirm` → `Destination(edit)` or `Amount(edit)`

### FeeSelectorRoute (Send internal)
- `ChooseToken` — select fee token
- `ChooseSpeed` — select fee speed

### WcInnerRoute (WalletConnect)
**File:** `features/walletconnect/impl/.../routing/WcInnerRoute.kt`

| Route | Description |
|-------|-------------|
| `Method.Send` | WC send transaction |
| `Method.SignMessage` | WC sign message |
| `Method.AddNetwork` | WC add network |
| `Method.SwitchNetwork` | WC switch network |
| `Pair` | WC pairing request |
| `UnsupportedMethodAlert` | Unsupported method alert |
| `WcDappDisconnected` | DApp disconnected alert |
| `TangemUnsupportedNetwork` | Unsupported network alert |
| `RequiredAddNetwork` | Required network add |
| `RequiredReconnectWithNetwork` | Required network reconnect |

### TangemPayDetailsInnerRoute
**File:** `features/tangempay/details/impl/.../navigation/TangemPayDetailsInnerRoute.kt`

| Route | Description |
|-------|-------------|
| `Details` | Main details view |
| `ChangePIN` | Change PIN flow |
| `ChangePINSuccess` | PIN change success |
| `AddToWallet` | Add card to device wallet |

Transitions: `Details` → `ChangePIN` → `ChangePINSuccess`, `Details` → `AddToWallet`

### FeedEntryRoute
**File:** `features/feed/api/.../components/FeedEntryRoute.kt`

| Route | Description |
|-------|-------------|
| `MarketTokenDetails` | Market token detail view |
| `MarketTokenList` | Markets list |
| `NewsDetail` | News article detail |

### CreateWalletBackupRoute
**File:** `features/hot-wallet/impl/.../createwalletbackup/routing/CreateWalletBackupRoute.kt`

| Route | Description |
|-------|-------------|
| `RecoveryPhraseStart` | Backup intro |
| `RecoveryPhrase` | Show recovery phrase |
| `ConfirmBackup` | Confirm backup |
| `BackupCompleted` | Backup complete (with upgrade/last screen flags) |

Transitions: `RecoveryPhraseStart` → `RecoveryPhrase` → `ConfirmBackup` → `BackupCompleted`

### AddExistingWalletRoute
**File:** `features/hot-wallet/impl/.../addexistingwallet/entry/routing/AddExistingWalletRoute.kt`

| Route | Description |
|-------|-------------|
| `Import` | Seed phrase import |
| `BackupCompleted` | Backup completed |
| `SetAccessCode` | Set access code |
| `ConfirmAccessCode` | Confirm access code |
| `PushNotifications` | Push notification opt-in |
| `SetupFinished` | Setup complete |

Transitions: `Import` → `BackupCompleted` → `SetAccessCode` → `ConfirmAccessCode` → `PushNotifications` → `SetupFinished`

### UpdateAccessCodeRoute
**File:** `features/hot-wallet/impl/.../updateaccesscode/routing/UpdateAccessCodeRoute.kt`

| Route | Description |
|-------|-------------|
| `SetAccessCode` | Enter new access code |
| `ConfirmAccessCode` | Confirm new access code |
| `SetupFinished` | Update complete |

Transitions: `SetAccessCode` → `ConfirmAccessCode` → `SetupFinished`

### WalletActivationRoute
**File:** `features/hot-wallet/impl/.../walletactivation/entry/routing/WalletActivationRoute.kt`

| Route | Description |
|-------|-------------|
| `ManualBackupStart` | Backup intro |
| `ManualBackupPhrase` | Show recovery phrase |
| `ManualBackupCheck` | Verify backup |
| `ManualBackupCompleted` | Backup success |
| `SetAccessCode` | Set access code |
| `ConfirmAccessCode` | Confirm access code |
| `PushNotifications` | Push notification opt-in |
| `SetupFinished` | Activation complete |

Transitions: `ManualBackupStart` → `ManualBackupPhrase` → `ManualBackupCheck` → `ManualBackupCompleted` → `SetAccessCode` → `ConfirmAccessCode` → `PushNotifications` → `SetupFinished`

## 4. Deep Links

### URI Schemes

| Scheme | Value | Usage |
|--------|-------|-------|
| `Tangem` | `tangem://` | Primary app deep links |
| `WalletConnect` | `wc://` | WalletConnect pairing |
| `Https` | `https://` | Web links (tangem.com) |

### Tangem Scheme Routes (`tangem://{host}`)

| Host | Handler | Target |
|------|---------|--------|
| `onramp` | `OnrampDeepLinkHandler` | Onramp callback processing |
| `redirect_sell` | `SellRedirectDeepLinkHandler` | `Send` (sell redirect with tx params) |
| `redirect` | — | Buy redirect (no-op) |
| `buy` | `BuyDeepLinkHandler` | `BuyCrypto` |
| `sell` | `SellDeepLinkHandler` | `SellCrypto` |
| `swap` | `SwapDeepLinkHandler` | `SwapCrypto` |
| `referral` | `ReferralDeepLinkHandler` | Referral flow |
| `main` | `WalletDeepLinkHandler` | Wallet screen |
| `token` | `TokenDetailsDeepLinkHandler` | `CurrencyDetails` |
| `staking` | `StakingDeepLinkHandler` | `Staking` |
| `markets` | `MarketsDeepLinkHandler` | `Markets` |
| `token_chart` | `MarketsTokenDetailDeepLinkHandler` | `MarketsTokenDetails` |
| `wc` | `WalletConnectDeepLinkHandler` | WalletConnect pairing |
| `promo` | `PromoDeeplinkHandler` | Promo handling |
| `onboard-visa` | `OnboardVisaDeepLinkHandler` | `TangemPayOnboarding` |

### HTTPS Routes (`https://tangem.com/...`)

| Path prefix | Handler | Target |
|-------------|---------|--------|
| `/pay-app` | `OnboardVisaDeepLinkHandler` | `TangemPayOnboarding` |
| `/news` | `NewsDetailsDeepLinkHandler` | `NewsDetails` |

### Deep Link Readiness

Deep links are only processed when the app is on a "ready" route. These routes **block** deep link processing:
- `Initial`, `Home`, `Welcome`, `PushNotification`, `Disclaimer`, `Stories`, `Onboarding`