# Redwood Layout

Redwood's layout base system provides base primitives for testing and app-prototipes. It provides widgets for:

- `Text`
- `Button widget`
- `TextInput widget`
- `Image widget`
- `BackgroundColor modifier`

//Internally the layout system uses a common layout engine written in Kotlin Multiplatform. The
//layout engine operates on a virtual DOM (document object model) composed of simple nodes
//where each node is mapped to a widget in the real DOM. This lets the layout engine perform
//operations on the DOM with consistent rendering across platforms. The system provides widget
//bindings for for Android Views (`redwood-layout-view`), iOS UiKit (`redwood-layout-uiview`), and
//Compose UI (`redwood-layout-composeui`).

//**[Check out here for documentation for each of `Column` and `Row`'s modifiers.](https://github.com/cashapp/redwood/blob/trunk/redwood-layout-schema/src/main/kotlin/app/cash/redwood/layout/modifiers.kt)**


//The `density` object is created differently depending on the platform.

//- On Android, you can create a density object using `Density(resources)`.
//- On iOS, you can get a density object using `Density.Default`.
//    - We don't need to create a custom density object as iOS already handles density in their coordinate system by default.
//- In Compose UI, you can create a density object using `Density(LocalDensity.current.density.toDouble())`.
