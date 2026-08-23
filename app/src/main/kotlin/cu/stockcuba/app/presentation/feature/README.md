# presentation/feature/ — Pantallas / Features (MVVM)

Una subcarpeta por **feature/pantalla** independiente. Cada feature contiene su UI + ViewModel + Estado.

## Estructura por feature
```
feature/
└── product/
    ├── ProductScreen.kt        # @Composable principal (stateless, recibe UiState + eventos)
    ├── ProductViewModel.kt     # ViewModel (HiltViewModel), expone UiState + handleEvent()
    ├── ProductUiState.kt       # sealed interface/ui data class para estado de pantalla
    └── ProductEvent.kt         # (opcional) sealed interface para eventos de UI → VM
```

## Convenciones MVVM
- **Screen**: `@Composable fun ProductScreen(viewModel: ProductViewModel = hiltViewModel(), onNavigate: ...)`
  - Observa `viewModel.uiState` via `collectAsStateWithLifecycle()`
  - Llama `viewModel.onEvent(ProductEvent.Load)` en `LaunchedEffect` o callbacks
  - **Sin lógica** — solo composición y delegación de eventos
- **ViewModel**: `class ProductViewModel @Inject constructor(private val getProductsUseCase: GetProductsUseCase) : ViewModel()`
  - `_uiState = MutableStateFlow(ProductUiState.Loading)`
  - `fun onEvent(event: ProductEvent) { when(event) { ... } }`
  - Usa `viewModelScope.launch { ... }` para trabajo asíncrono
- **UiState**: `sealed interface ProductUiState { data class Success(val products: List<Product>) : ProductUiState; object Loading : ProductUiState; data class Error(val message: String) : ProductUiState }`
- **Event**: `sealed interface ProductEvent { data class Load(val category: Category?) : ProductEvent; object Retry : ProductEvent }`

## Inyección
- `@HiltViewModel` en ViewModel
- `hiltViewModel()` en Screen
- UseCases inyectados en ViewModel constructor

## Testing
- ViewModel: test unitario con `MockK`, verifica `uiState` transitions
- Screen: Compose UI test (`composeTestRule`), verifica rendering dado `UiState`