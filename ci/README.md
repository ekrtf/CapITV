# CI workflow (not active yet)

`github-actions-ci.yml` is the full GitHub Actions pipeline: shellcheck and fake-adb tests
for the provisioning script, then `assembleRelease testReleaseUnitTest lintRelease`, an APK
permission check (it fails if INTERNET appears) and a GitHub release with the APK on every `v*` tag.

It lives here because the token that created the repo had no `workflow` scope, and GitHub
refuses pushes to `.github/workflows/` without it. To switch it on, run this once from a machine
logged in to `gh` as ekrtf:

```sh
gh auth refresh -h github.com -s workflow     # browser sign-in, adds the scope
mkdir -p .github/workflows
git mv ci/github-actions-ci.yml .github/workflows/ci.yml
git commit -m "Enable CI" && git push
```

The signing secrets (`CAPITV_KEYSTORE_BASE64`, `CAPITV_KEYSTORE_PASSWORD`, `CAPITV_KEY_ALIAS`,
`CAPITV_KEY_PASSWORD`) are already set on the repo, so CI signs with the same key as v0.1.0.
