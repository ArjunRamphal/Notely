## 2023-10-27 - Information Leakage in Cryptographic Catch Blocks
**Vulnerability:** Cryptographic functions (encryption/decryption) and key storage operations were using `e.printStackTrace()` and directly bubbling up `e.message` to the UI when exceptions occurred.
**Learning:** This exposes detailed cryptographic failures (like `AEADBadTagException` or file path `Permission denied`) to the system logs and the end-user, revealing implementation details and system state.
**Prevention:** Always use generic error messages for end-users when cryptographic operations fail (e.g., "An error occurred during encryption"), and avoid blindly printing stack traces for sensitive operations to standard output or Logcat.
## 2024-05-24 - Biometric Authentication Bypass
**Vulnerability:** BiometricPrompt was implemented without a CryptoObject, allowing potential framework bypasses (e.g. Frida hooks) to manually trigger success callbacks.
**Learning:** Implementing `setUserAuthenticationRequired(true)` on Keystore AES keys and wrapping the Cipher in a CryptoObject cryptographically binds authentication to the hardware.
**Prevention:** Ensure `cryptoObject!!.cipher!!.doFinal()` is explicitly executed in `onAuthenticationSucceeded` and null-checked to trigger an exception on bypass attempts.
