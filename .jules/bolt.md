## 2024-10-28 - Dynamic DB Sorting with un-keyed LazyColumn
**Learning:** Toggling favorite status triggers dynamic DB sorting. Combined with an un-keyed LazyColumn, this causes O(N) re-renders.
**Action:** Provide stable keys in Lazy lists.
## 2026-10-30 - Dynamic Tags Sorting with un-keyed LazyRow
**Learning:** Parsing tags from a string without distinct keys can cause duplicate key crashes and O(N) re-renders in LazyRow.
**Action:** Use .distinct() and stable keys in Lazy lists.
## 2026-11-01 - Optimize Search Filtering
**Learning:** Unconditional O(N) list filtering triggers unnecessary CPU and memory allocation on every list update when the search query is empty.
**Action:** Add early return inside derived state calculations when the query is blank to return the original reference.
## 2026-11-03 - Avoid Lambda Capture in Compose
**Learning:** Capturing outer iteration variables inside lambdas passed to child composables (e.g., in LazyColumn) causes unnecessary O(N) recompositions on every list update.
**Action:** Use the inner lambda's iteration variable (e.g., `it`) instead of capturing the outer scope.
## 2024-11-06 - Memoize SimpleDateFormat
**Learning:** Instantiating SimpleDateFormat inside list items (like NoteCard) causes unnecessary allocations during recomposition.
**Action:** Memoize using remember.
## 2026-11-06 - Hoist getAnnotatedString from VisualTransformation
**Learning:** Instantiating or building an AnnotatedString inside a VisualTransformation lambda causes it to be rebuilt on every layout/measure pass, even if the text/styles haven't changed.
**Action:** Hoist the getAnnotatedString call out of the lambda and into the enclosing remember block so it is only rebuilt when the state changes.
