# di/ — Hilt Dependency Injection Modules

Módulos de Hilt para proporcionar dependencias a lo largo de la app.

## Estructura esperada
- `DatabaseModule.kt` — provee `RoomDatabase`, DAOs
- `NetworkModule.kt` — provee `Retrofit`, `OkHttpClient`, `Moshi`
- `RepositoryModule.kt` — bindea interfaces de `domain.repository` a implementaciones de `data.repository`
- `UseCaseModule.kt` — (opcional) provee casos de uso si se quiere inyectar en lugar de crear en ViewModel
- `DataStoreModule.kt` — provee `DataStore<Preferences>`

## Convención
- Un módulo por capa/preocupación
- Anotados con `@InstallIn(SingletonComponent::class)` o el componente adecuado
- Usar `@Provides` o `@Binds` según corresponda