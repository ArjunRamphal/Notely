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

## 2025-02-09 - Persistent Lockout Vulnerability
**Vulnerability:** The PIN lockout mechanism solely used `SystemClock.elapsedRealtime()` for tracking the lockout duration across the device lifecycle. If a user is locked out and reboots their device, the `elapsedRealtime()` resets to zero, but the persisted `LOCKOUT_TIMESTAMP` remains unchanged. Since `now < LOCKOUT_TIMESTAMP` remains perpetually true after a reboot, the lockout becomes permanent.
**Learning:** For security-critical timers that must survive reboots, you cannot solely rely on monotonic time (`SystemClock.elapsedRealtime()`) because it resets on reboot. While it protects against system time manipulation, its reset behavior can cause deadlocks if persisted directly.
**Prevention:** Use a hybrid approach for persistent lockouts: persist the `elapsedRealtime() + duration`, the `duration` itself, and a fallback wall-clock timestamp (`System.currentTimeMillis() + duration`). Detect reboots by checking if `lockoutTime - now > duration`, and safely fallback to checking the wall-clock time if a reboot is detected.
