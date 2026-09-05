# Signing

Every build — CI, local, debug and release — is signed with the key in this
folder so that APKs can always be installed over each other. Android refuses to
update an app when the signature changes, which is why the key is committed:
the plugin is distributed as a debug APK from GitHub Actions, and a rotating
per-machine key would force a full uninstall (and the loss of every target's
settings) on each update.

| | |
|---|---|
| File | `signing/ainews.p12` (PKCS#12) |
| Store password | `ainews` |
| Key alias | `ainews` |
| Key password | `ainews` |
| Valid until | 2056 |

This key proves nothing about authorship — treat it as a public convenience
key, not a secret.

## Using your own key

Create `signing.properties` in the repository root (it is git-ignored):

```properties
storeFile=/absolute/path/to/your.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Or set `AI_NEWS_KEYSTORE`, `AI_NEWS_KEYSTORE_PASSWORD`, `AI_NEWS_KEY_ALIAS` and
`AI_NEWS_KEY_PASSWORD` in the environment — handy for CI secrets. A JKS file
works too; the `storeType` in `app/build.gradle.kts` then needs no change,
Gradle detects it.
