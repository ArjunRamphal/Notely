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
## 2025-02-14 - Fix Permanent Lockout via SystemClock.elapsedRealtime in SharedPreferences
**Vulnerability:** The application used `SystemClock.elapsedRealtime()` to store the `LOCKOUT_TIMESTAMP` in `SharedPreferences`. Because `elapsedRealtime()` resets to 0 upon device reboot, the newly booted device will compare a very large timestamp against a small, freshly reset uptime. This results in users being permanently locked out of the app across device reboots.
**Learning:** While `SystemClock.elapsedRealtime()` is essential for preventing lockout bypass via system time manipulation, it must **ONLY** be used for in-memory tracking. It is completely unsuited for persistent storage since the frame of reference resets across boot cycles.
**Prevention:** For persistent storage involving dates or timestamps across application restarts, use `System.currentTimeMillis()` or similar epoch-based time. Use `elapsedRealtime()` strictly for active, in-session duration tracking.
