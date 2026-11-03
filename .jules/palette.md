## 2024-10-30 - Added Empty State
**Learning:** Implementing explicit empty states in lists (especially search results or empty lists) is a crucial UX improvement, providing context instead of displaying a blank screen.
**Action:** Always verify empty states for list/search screens.
## 2026-11-01 - Add clear button to Search Bar
**Learning:** Users often need a quick way to clear search queries. Providing a dedicated clear button in the search bar improves usability and saves them from deleting text manually.
**Action:** Always include a clear action (trailing icon) in search input fields when they contain text.

## 2024-11-03 - Dynamic Content Descriptions for Toggleable Icons
**Learning:** Screen readers need accurate feedback for toggleable actions. Using static labels like "Toggle Theme" is ambiguous; the description should announce what the action *will* do.
**Action:** Always use dynamic, state-aware `contentDescription` values for toggleable icons (e.g., 'Switch to Dark Theme' vs 'Switch to Light Theme') instead of static labels.
