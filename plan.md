1. **Analyze `.jules/palette.md` for UX/accessibility instructions:**
   - I see this specific pattern to fix: "In Jetpack Compose, when placing a `Switch` inside a `ListItem` alongside text, set the `Switch`'s `onCheckedChange` parameter to `null` and apply `Modifier.toggleable(value = ..., onValueChange = { ... }, role = Role.Switch)` to the parent `ListItem`. This expands the tap target to the entire row and improves screen reader accessibility."

2. **Modify `app/src/main/java/com/example/notely/MainActivity.kt`:**
   - There are multiple `ListItem` components containing a `Switch`. I will update each of them to use `Modifier.toggleable` on the `ListItem` itself instead of `Switch(onCheckedChange = ...)`, and set the `Switch`'s `onCheckedChange` to `null`.
   - Affected ListItems:
     - PIN Lockout Timer
     - Scramble PIN Keypad
     - Biometric Unlock
     - Clear Clipboard on Exit
     - Auto-Save Notes

3. **Verify changes:**
   - Use `read_file` to verify the modified `MainActivity.kt`.
   - Run compilation `./gradlew assembleDebug` to ensure there are no syntax or typing errors.

4. **Complete pre-commit steps:**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

5. **Submit the PR:**
   - PR Title: `🎨 Palette: [UX improvement] Expand tap targets for Switch list items`
   - PR Description with 💡 What, 🎯 Why, 📸 Before/After, ♿ Accessibility.
