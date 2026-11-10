# APK signing

Android only lets an app update another with the **same signing key**. CapITV handles this as follows:

| Where | Key used |
|---|---|
| CI, when the repository secrets below are set (they are on `ekrtf/CapITV`) | The **CapITV release key**, so every release can update the previous one |
| CI without the secrets (e.g. a fork) | A throwaway **debug key** generated on the runner. Works, but each build has a different key |
| Local `./gradlew assembleRelease` | `launcher/keystore.properties` if present, otherwise your machine's debug key |

Secrets: `CAPITV_KEYSTORE_BASE64` (base64 of the `.jks`), `CAPITV_KEYSTORE_PASSWORD`,
`CAPITV_KEY_ALIAS`, `CAPITV_KEY_PASSWORD`. The keystore file and passwords are
**never committed** (`*.jks`, `keystore.properties` are git-ignored).

This is a personal, sideloaded app, so the release key is a self-generated key and isn't registered with any store.

If a box already has CapITV signed with a different key, `capitv-provision.sh`
detects `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstalls **only CapITV** and
reinstalls it. CapITV keeps no user data, so nothing is lost.

To create your own key:

```sh
keytool -genkeypair -v -keystore capitv-release.jks -alias capitv \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=CapITV"
base64 < capitv-release.jks | gh secret set CAPITV_KEYSTORE_BASE64 --repo ekrtf/CapITV
gh secret set CAPITV_KEY_ALIAS --repo ekrtf/CapITV --body capitv
gh secret set CAPITV_KEYSTORE_PASSWORD --repo ekrtf/CapITV   # prompts
gh secret set CAPITV_KEY_PASSWORD --repo ekrtf/CapITV        # prompts
```

For local release builds, create `launcher/keystore.properties`:

```properties
storeFile=/absolute/path/capitv-release.jks
storePassword=...
keyAlias=capitv
keyPassword=...
```
