# FlowStore

```
@FlowStore
data class UiState(
    val text: String = "1234",
    val count: Int = 0
)

// generated
public class UiStateStore private constructor(
  state: UiState
) {
  private val _flow: MutableStateFlow<UiState> = MutableStateFlow(state)

  public val flow: StateFlow<UiState>
    get() = _flow

  public suspend fun text(block: String.() -> String): Unit {
    val oldState = _flow.value
    val oldValue = oldState.text
    val newValue = oldValue.block()
    val newState = oldState.copy(text = newValue)
    _flow.emit(newState)
  }

  public suspend fun count(block: Int.() -> Int): Unit {
    val oldState = _flow.value
    val oldValue = oldState.count
    val newValue = oldValue.block()
    val newState = oldState.copy(count = newValue)
    _flow.emit(newState)
  }

  public companion object {
    public fun UiState.asStore() = UiStateStore(state = this)
  }
}
```

```
// app/build.gradle

plugins {
    ...
    id 'idea'
}

repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.kzaemrio.FlowStore:annotation:0.0.3"
    ksp("com.github.kzaemrio.FlowStore:compiler:0.0.3")
}

afterEvaluate {
    idea {
        android.sourceSets.forEach { sourceSet ->
            sourceSet.kotlin {
                srcDir("build/generated/ksp/${sourceSet.name}/kotlin")
            }
        }
    }
}
```