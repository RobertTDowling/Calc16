# Calc16

“RPN calculator for Software Engineering”

* Kotlin, SDK 34
* Material 3 UI, Themes, Styles
* Jetpack Compose, `LazyListState`
* `ViewModel`, `StateFlow`, Coroutines, Threads
* `Room`, SQLite, `LiveData`
* `JUnit` Host tests for ViewModel

Redux of 2015 RPNCalcN Android Calculator using 2024 best practices

* Ground-up rewrite of Calc15 in Kotlin using `@Composable`
* Migrated keyboard from PNG `IconButtons` to clickable Text boxes using `AnnotatedString` for typographical effects
* Use of Material3 Theme for font sizes, color palette, and shapes
* Best practice use of `collectAsStateWithLifecycle` in `@Composable`
* Best practice use of `.stateIn` converting cold `Flow` to hot `StateFlow` in `ViewModel`
* Dependency injection for `ViewModel`
* Best practice super brief `MainActivity.onCreate()`
```
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        val viewModel: CalcViewModel = viewModel(factory = AppViewModelProvider.Factory)
        TheScaffold(viewModel)
    }
}
```
