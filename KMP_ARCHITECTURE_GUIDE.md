# KMP Architecture Guide — Native UI + Shared Business Logic

> Give this file to Claude when starting a new KMP project to establish the architecture.

## Architecture Summary

3-layer Kotlin Multiplatform app:
- `shared` module — pure Kotlin: data models, repository interfaces, use cases
- `composeApp` — Android app with Jetpack Compose UI
- `iosApp` — iOS app with SwiftUI

No shared UI. No expect/actual. No shared database layer.

```
┌─────────────────────┐     ┌─────────────────────┐
│   Android (Compose)  │     │    iOS (SwiftUI)     │
│  Screen → ViewModel  │     │  View → ViewModel    │
│  → Platform Repos    │     │  → Platform Repos    │
└────────┬────────────┘     └────────┬────────────┘
         └───────────┬───────────────┘
         ┌───────────▼───────────────┐
         │     shared (KMP Module)    │
         │  Models + Repo Interfaces  │
         │  + Use Cases               │
         └───────────────────────────┘
```

---

## Project Structure

```
project-root/
├── build.gradle.kts                     # Root build file with plugin aliases
├── settings.gradle.kts                  # Includes :composeApp and :shared
├── gradle/libs.versions.toml            # Version catalog for all dependencies
│
├── shared/                              # KMP LIBRARY MODULE (pure Kotlin)
│   ├── build.gradle.kts                 # KMP plugin + CocoaPods for iOS
│   ├── shared.podspec                   # Generated podspec for iOS consumption
│   └── src/commonMain/kotlin/com/app/
│       ├── model/                       # Data classes used by both platforms
│       ├── repository/                  # Interfaces ONLY — no implementations
│       ├── usecase/                     # Business logic — depends only on interfaces
│       ├── config/                      # Shared constants/configuration
│       └── util/                        # Pure Kotlin utilities
│
├── composeApp/                          # ANDROID APP MODULE
│   ├── build.gradle.kts                 # Android + KMP config
│   └── src/androidMain/
│       ├── AndroidManifest.xml
│       ├── res/                         # Android resources
│       └── kotlin/com/app/
│           ├── MainActivity.kt          # Entry point (ComponentActivity)
│           ├── App.kt                   # Root @Composable + NavHost with all routes
│           ├── navigation/              # Route sealed class definitions
│           ├── repository/              # Android implementations of shared interfaces
│           ├── viewmodel/               # AndroidViewModel + StateFlow
│           ├── ui/                      # Compose screens and components
│           │   ├── components/          # Reusable Compose components
│           │   └── [feature]/           # One package per feature
│           └── util/                    # Android-specific utilities
│
└── iosApp/                              # IOS XCODE PROJECT
    ├── Podfile                          # CocoaPods: shared framework + dependencies
    ├── iosApp.xcworkspace               # Open this (not .xcodeproj)
    └── iosApp/
        ├── iOSApp.swift                 # @main App + AppDelegate
        ├── ContentView.swift            # Root view + navigation root
        ├── navigation/
        │   └── NavigationState.swift    # Route enums
        ├── repository/                  # Swift implementations of shared interfaces
        ├── viewmodel/                   # @MainActor ObservableObject classes
        ├── ui/                          # SwiftUI views
        │   ├── components/              # Reusable SwiftUI components
        │   └── [FeatureView].swift      # One file per screen
        └── util/                        # iOS-specific utilities
```

---

## Layer-by-Layer Rules

### Layer 1: `shared/model/` — Data Classes

```kotlin
// shared/src/commonMain/kotlin/com/app/model/Item.kt
data class Item(
    val id: String = "",
    val title: String = "",
    val createdAt: Long = 0L
)
```

- Pure Kotlin — zero platform imports
- Default values on all fields (backend deserialization requires this)
- Enums include string conversion helpers for backend compatibility
- Separate "input" models for creation vs read models where they diverge

### Layer 2: `shared/repository/` — Interfaces Only

```kotlin
// shared/src/commonMain/kotlin/com/app/repository/ItemRepository.kt
interface ItemRepository {
    suspend fun getItem(id: String): Item?
    suspend fun createItem(item: Item): String
    suspend fun deleteItem(id: String)
}
```

- All methods are `suspend`
- Accept/return shared model types only
- No implementation code — just the contract
- One interface per domain area

### Layer 3: `shared/usecase/` — Business Logic

```kotlin
// shared/src/commonMain/kotlin/com/app/usecase/CreateItemUseCase.kt
class CreateItemUseCase(
    private val itemRepository: ItemRepository
) {
    suspend fun execute(title: String): String {
        require(title.isNotBlank()) { "Title cannot be blank" }
        val item = Item(title = title, createdAt = System.currentTimeMillis())
        return itemRepository.createItem(item)
    }
}
```

- One class per operation (Single Responsibility)
- Constructor takes ONLY repository interfaces — never platform classes
- Contains: input validation, conditional logic, orchestration across multiple repos
- NO platform imports, NO backend/database code
- Naming pattern: `VerbNounUseCase`

### Layer 4: Platform `repository/` — Implementations

**Android:**
```kotlin
// composeApp/src/androidMain/.../repository/BackendItemRepository.kt
class BackendItemRepository(
    private val authRepository: AuthRepository
) : ItemRepository {

    override suspend fun createItem(item: Item): String {
        // Use Android-native SDK to persist data
        val doc = db.collection("items").add(item.toMap()).await()
        return doc.id
    }
}
```

**iOS:**
```swift
// iosApp/iosApp/repository/BackendItemRepository.swift
class BackendItemRepository: ItemRepository {

    func createItem(item: Item) async throws -> String {
        // Use iOS-native SDK to persist data
        let ref = try await db.collection("items").addDocument(data: item.toDict())
        return ref.documentID
    }
}
```

- Both platforms implement the same interface, using their native SDKs
- Same backend paths/endpoints on both platforms
- Naming: `Android*Repository` / `IOS*Repository` for platform-specific capabilities (Auth, Storage, Speech, Location); `Backend*Repository` for data repos that are mirrored on both platforms

### Layer 5: Platform `viewmodel/` — State Management

**Android:**
```kotlin
class FeatureViewModel(application: Application) : AndroidViewModel(application) {
    // Manual DI chain
    private val authRepo = AndroidAuthRepository(context)
    private val itemRepo = BackendItemRepository(authRepo)
    private val createItemUseCase = CreateItemUseCase(itemRepo)  // shared use case

    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    fun onCreateItem(title: String) {
        viewModelScope.launch {
            val id = createItemUseCase.execute(title)
            _uiState.update { it.copy(createdId = id) }
        }
    }
}
```

**iOS:**
```swift
@MainActor
class FeatureViewModel: ObservableObject {
    private let createItemUseCase: CreateItemUseCase
    @Published var createdId: String?

    init() {
        // Manual DI chain
        let authRepo = IOSAuthRepository()
        let itemRepo = BackendItemRepository(authRepository: authRepo)
        self.createItemUseCase = CreateItemUseCase(itemRepository: itemRepo)  // shared use case
    }

    func onCreateItem(title: String) {
        Task {
            let id = try await createItemUseCase.execute(title: title)
            self.createdId = id
        }
    }
}
```

- Manual DI — each ViewModel builds its own dependency chain in its constructor
- No DI framework (no Koin, no Hilt)
- Repos are fresh instances per ViewModel (not singletons)
- Android: `AndroidViewModel` + `StateFlow` + `viewModelScope.launch`
- iOS: `@MainActor ObservableObject` + `@Published` + `Task { }`

### Layer 6: Platform `ui/` — Native Screens

**Android (Compose):**
```kotlin
@Composable
fun FeatureScreen(viewModel: FeatureViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Button(onClick = { viewModel.onCreateItem("Hello") }) {
        Text("Create")
    }
}
```

**iOS (SwiftUI):**
```swift
struct FeatureView: View {
    @StateObject private var viewModel = FeatureViewModel()
    var body: some View {
        Button("Create") { viewModel.onCreateItem(title: "Hello") }
    }
}
```

---

## Navigation

**Android:** Jetpack Navigation Compose
- Sealed class/object for all route definitions (with typed parameters)
- Single `NavHost` in `App.kt` listing every `composable()` destination
- Parameters passed as URL-encoded route strings

**iOS:** SwiftUI NavigationStack
- Two enums: `Screen` for auth flow (manual stack management), `AppDestination: Hashable` for in-app navigation (NavigationStack path)
- `TabView` at the root with a separate `NavigationStack` per tab

---

## Adding a New Feature — Step by Step

1. **`shared/model/`** — add/update data classes if needed
2. **`shared/repository/`** — add methods to an existing interface or create a new one
3. **`shared/usecase/`** — create the use case class with business logic
4. **Android `repository/`** — implement the interface using Android SDK
5. **iOS `repository/`** — implement the same interface using iOS SDK
6. **Android `viewmodel/`** — create/update ViewModel, wire DI, expose state
7. **iOS `viewmodel/`** — create/update ViewModel, wire DI, expose state
8. **Android `ui/`** — build the Compose screen
9. **iOS `ui/`** — build the SwiftUI view
10. **Navigation** — register routes on both platforms

---

## What "Shared" Actually Saves You

| Change type | Files touched |
|---|---|
| Pure logic change (add validation, change a condition) | 1 — the use case |
| New business rule using existing repo methods | 1 — the use case |
| New repo method needed | 1 interface + 2 implementations + 2 ViewModel wirings ≈ 5 files |
| New feature end-to-end | model + interface + use case + 2 repos + 2 VMs + 2 UI screens ≈ 9 files |

Shared = shared **decisions**. Not shared **plumbing**.

---

## DO / DON'T

**DO:**
- Put all data classes in `shared/model/`
- Put all repository interfaces in `shared/repository/`
- Put all business logic in `shared/usecase/`
- Keep use cases pure — only depend on repository interfaces
- Implement repos separately on each platform using native SDKs
- Mirror feature parity: same screens on both platforms, native implementation each

**DON'T:**
- Don't put any platform import in the shared module
- Don't share ViewModels across platforms
- Don't share UI code
- Don't put backend/database logic in use cases
- Don't use expect/actual — platform code lives in the platform layers
- Don't use a DI framework — manual constructor injection only
- Don't make repository implementations singletons
- Don't skip the use case layer even for simple operations — consistency pays off
