# GitHub Actions CI/CD for StockCuba

This project uses GitHub Actions for continuous integration and delivery, avoiding corporate TLS interception issues.

## Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `android.yml` | Push, PR | Full build, test, lint, artifact upload |
| `pr-check.yml` | PR only | Fast feedback on PRs |
| Release | Tag `v*` | Signed AAB + GitHub Release |

## Quick Start

### 1. Push to GitHub
```bash
git add .github/workflows/
git commit -m "ci: add GitHub Actions workflows"
git push origin main
```

### 2. Verify Build
- Go to **Actions** tab in GitHub
- Watch the build run on clean Ubuntu runners
- Download APK artifacts from the run summary

### 3. Enable Signed Releases (optional)
1. Add secrets in **Settings > Secrets and variables > Actions**:
   - `KEYSTORE_BASE64` - base64 encoded keystore
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
2. Create a tag: `git tag v1.0.0 && git push origin v1.0.0`
3. A signed AAB will be created and attached to GitHub Release

## Artifacts Produced

| Artifact | Path | Retention |
|----------|------|-----------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | 7 days |
| Release APK (unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | 7 days |
| Release Bundle (signed if secrets) | `app/build/outputs/bundle/release/app-release.aab` | 30 days |
| Lint Report | `app/build/reports/lint-results-debug.html` | 7 days |
| Test Reports | `app/build/reports/tests/` | 7 days |

## Why This Works

- **Clean runners**: GitHub-hosted Ubuntu runners have no corporate TLS interception
- **Direct internet access**: Direct access to `dl.google.com`, `plugins.gradle.org`, `mavenCentral()`
- **No SSL issues**: No MITM certificate problems
- **Free tier**: 2000 min/month on public repos

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Build timeout | Increase `timeout-minutes` in workflow |
| Out of memory | Add `-Xmx4g` to GRADLE_OPTS |
| Lint errors | Run locally: `./gradlew lint` |
| Test failures | Check test reports artifact |

## Local Development Still Works

The GitHub Actions setup doesn't affect local builds. Continue using:
```bash
./gradlew assembleDebug
./gradlew test
```