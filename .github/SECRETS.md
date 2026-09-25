# GitHub Actions Secrets Required for Signed Release Builds

## Required Secrets (Settings > Secrets and variables > Actions)

### Keystore (for signed APK/AAB)
```
KEYSTORE_BASE64          # Base64 encoded keystore file
KEYSTORE_PASSWORD        # Keystore password
KEY_ALIAS                # Key alias
KEY_PASSWORD             # Key password
```

### Optional: Code Signing
```
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  # For Play Store deployment
```

---

## How to Generate Base64 Keystore

```bash
# On Linux/macOS:
base64 -w 0 stockcuba-release.keystore > keystore_base64.txt
cat keystore_base64.txt | pbcopy  # macOS
cat keystore_base64.txt | clip    # Windows
```

Then paste the output in GitHub Secret `KEYSTORE_BASE64`.

---

## Workflow with Signing (add to android.yml)

Add this step before "Build Release Bundle":

```yaml
      - name: Decode Keystore
        if: env.KEYSTORE_BASE64 != ''
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}

      - name: Set Signing Config
        if: env.KEYSTORE_BASE64 != ''
        run: |
          cat >> app/signing.properties <<EOF
          storeFile=../keystore.jks
          storePassword=${{ secrets.KEYSTORE_PASSWORD }}
          keyAlias=${{ secrets.KEY_ALIAS }}
          keyPassword=${{ secrets.KEY_PASSWORD }}
          EOF
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Build Signed Release Bundle
        if: env.KEYSTORE_BASE64 != ''
        run: ./gradlew bundleRelease --no-daemon --stacktrace
```

---

## Usage

1. Go to your repo: **Settings > Secrets and variables > Actions**
2. Click **New repository secret** for each secret above
3. Push a tag: `git tag v1.0.0 && git push origin v1.0.0`
4. The release workflow will create a signed AAB and GitHub Release automatically