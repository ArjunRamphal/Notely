## 2024-05-24 - Provide Stable Keys for LazyColumn items
**Learning:** In Jetpack Compose, always provide a stable `key` parameter to `items()` within `LazyColumn` or `LazyRow` to prevent unnecessary O(N) recompositions, especially when lists are dynamically sorted.
**Action:** When using `items` or `itemsIndexed` in Jetpack Compose, always explicitly specify the `key` parameter if a unique identifier is available, avoiding default positional keys which break during reordering.
