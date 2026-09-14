## 2024-05-24 - [System Time Manipulation Bypass]
**Vulnerability:** Auto-lock timer used `System.currentTimeMillis()` for tracking application background time, allowing users to bypass app lock requirements by modifying device wall-clock time in system settings.
**Learning:** `System.currentTimeMillis()` can be manipulated by users, whereas `SystemClock.elapsedRealtime()` provides a strictly monotonic clock resistant to tampering. However, replacing it must be done carefully; persisting `SystemClock.elapsedRealtime()` across reboots (e.g., in SharedPreferences) will cause severe lockouts as it resets to zero, so it is only safe for in-memory comparisons (like ViewModels).
**Prevention:** Use `SystemClock.elapsedRealtime()` for tracking durations and security lockouts that are only kept in memory.
## 2023-10-27 - Information Leakage in Cryptographic Catch Blocks
**Vulnerability:** Cryptographic functions (encryption/decryption) and key storage operations were using `e.printStackTrace()` and directly bubbling up `e.message` to the UI when exceptions occurred.
**Learning:** This exposes detailed cryptographic failures (like `AEADBadTagException` or file path `Permission denied`) to the system logs and the end-user, revealing implementation details and system state.
**Prevention:** Always use generic error messages for end-users when cryptographic operations fail (e.g., "An error occurred during encryption"), and avoid blindly printing stack traces for sensitive operations to standard output or Logcat.
## 2024-05-24 - Biometric Authentication Bypass
**Vulnerability:** BiometricPrompt was implemented without a CryptoObject, allowing potential framework bypasses (e.g. Frida hooks) to manually trigger success callbacks.
**Learning:** Implementing `setUserAuthenticationRequired(true)` on Keystore AES keys and wrapping the Cipher in a CryptoObject cryptographically binds authentication to the hardware.
**Prevention:** Ensure `cryptoObject!!.cipher!!.doFinal()` is explicitly executed in `onAuthenticationSucceeded` and null-checked to trigger an exception on bypass attempts.
\n## 2025-02-09 - Lockout Bypass via Device Time Manipulation\n**Vulnerability:** The PIN lockout duration was calculated using `System.currentTimeMillis()`, which can be manipulated by changing the device's system time, allowing a user to bypass the lockout period.\n**Learning:** Security-critical timeouts and durations should never rely on user-configurable time sources.\n**Prevention:** Use `SystemClock.elapsedRealtime()` for measuring elapsed time and timeouts in Android, as it is guaranteed to be monotonic and cannot be altered by the user.

## 2025-02-09 - Biometric Key Invalidation Crash
**Vulnerability:** Keystore keys generated with `setUserAuthenticationRequired(true)` are permanently invalidated by Android if the user changes or removes their enrolled biometrics, causing unhandled `KeyPermanentlyInvalidatedException` on subsequent cipher initializations and locking the user out.
**Learning:** Keys tied to biometric authentication have a volatile lifecycle.
**Prevention:** Always catch `KeyPermanentlyInvalidatedException` when initializing a `Cipher` for biometric keys, delete the invalidated key alias from the Keystore, and optionally re-generate a new key before proceeding.
## 2026-09-14 - Prevent permanent lockout vulnerability on reboot
**Vulnerability:** Relying solely on `SystemClock.elapsedRealtime()` for lockout durations causes a permanent lockout (DoS) upon device reboot because the elapsed time resets to 0.
**Learning:** Security-critical timeouts should use `elapsedRealtime()` to prevent system time manipulation, but must also persist the lockout duration and a wall-clock fallback (`System.currentTimeMillis()`) to handle reboots safely.
**Prevention:** Use a hybrid approach: save the monotonic timestamp, duration, and wall-clock fallback. If `lockoutTime - now > duration` indicates a reboot, fallback to the wall-clock timestamp.
