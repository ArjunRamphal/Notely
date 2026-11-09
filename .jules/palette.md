## 2024-10-30 - Added Empty State
**Learning:** Implementing explicit empty states in lists (especially search results or empty lists) is a crucial UX improvement, providing context instead of displaying a blank screen.
**Action:** Always verify empty states for list/search screens.
## 2026-11-01 - Add clear button to Search Bar
**Learning:** Users often need a quick way to clear search queries. Providing a dedicated clear button in the search bar improves usability and saves them from deleting text manually.
**Action:** Always include a clear action (trailing icon) in search input fields when they contain text.
## 2024-05-24 - Dynamic Accessibility Labels
**Learning:** Static labels for stateful toggle buttons (like 'Favorite' or 'Toggle Theme') leave screen reader users without feedback about the current state or resulting action.
**Action:** Always use dynamic, state-aware 'contentDescription' values (e.g., 'Add to favorites' vs 'Remove from favorites') for toggle controls.
## 2026-11-04 - Password Visibility Toggle
**Learning:** Users experience high friction and error rates when entering passwords blindly. Providing a visibility toggle significantly improves the UX of password entry fields, especially in sensitive contexts like backups.
**Action:** Always include a visibility toggle (e.g., using `VisualTransformation` and a trailing `IconButton`) for password input fields.
## 2026-11-05 - Add clearAndSetSemantics for standalone text icons
**Learning:** Standalone text characters (like 'B' or 'I') inside icon buttons without an explicit contentDescription fail to provide context to screen readers.
**Action:** Use `Modifier.clearAndSetSemantics { contentDescription = '...' }` on standalone text characters functioning as icons to provide accurate feedback to screen readers.
## 2024-05-24 - Make ListItems toggleable
**Learning:** Setting Switch onCheckedChange to null and adding toggleable to the parent ListItem expands the tap target and improves accessibility.
**Action:** Use this pattern whenever a Switch is presented alongside explanatory text.
## 2026-11-09 - Disabled States & Empty Fallbacks
**Learning:** Silently ignoring button clicks when required inputs are empty (e.g., Save Note, Export Password) creates confusing "dead clicks". Additionally, `Text` composables with empty strings can collapse or leave awkward gaps in list items.
**Action:** Always bind the `enabled` state of interactive elements to their validation logic instead of silently failing inside `onClick`. Provide styled fallback strings (e.g., "Untitled Note" in lower opacity) for optional text fields displayed in lists.
