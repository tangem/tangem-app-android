# core/datasource

## API Integration Guide

### Config Structure

- `ApiConfig` — base API config with `id` (`ApiConfig.ID`), `defaultEnvironment` (`ApiEnvironment`), and `environmentConfigs` (list of `ApiEnvironmentConfig`)
- `ApiEnvironmentConfig` — per-environment settings: `environment`, `baseUrl`, and `headers` (map of header name to `Provider<String>`)

### Config Management

- `ApiConfigsManager` — DI-available component for accessing configs via `getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig`
- Two implementations: `ProdApiConfigsManager` (release) and `DevApiConfigsManager` (extends `MutableApiConfigsManager`, used when `BuildConfig.TESTER_MENU_ENABLED`)
- `MutableApiConfigsManager` allows runtime environment switching via Tester Menu without app restart

### Adding a New API

1. Create `ApiConfig` subclass in `com.tangem.datasource.api.common.config` — override `defaultEnvironment` and `environmentConfigs`. DI dependencies can be injected via constructor
2. Register the new config ID in `ApiConfig.initializeId(...)`
3. Provide the config in `ApiConfigsModule` using `@Provides @IntoSet`
4. Provide the API Retrofit service in `NetworkModule`:
   - Get environment config: `apiConfigsManager.getEnvironmentConfig(id)`
   - Use `environmentConfig.baseUrl` for Retrofit base URL
   - Apply headers via `OkHttpClient.Builder().applyApiConfig(id, apiConfigsManager)`

### Testing

- Add the new config to `API_CONFIGS` list in `ProdApiConfigsManagerTest`
- Add a test model in the `data` method with expected `ApiEnvironmentConfig` values
- If the config has constructor dependencies, mock them and set up behavior in `setup()`