# Submitting Castlist to F-Droid

F-Droid adds new apps through a merge request (MR) to the `fdroiddata` repository on GitLab. The recipe is `io.github.dcdude71.castlist.yml` in this folder. F-Droid builds the app itself from the `v1.0.0` tag and signs it with its own key.

## Before you start (once)

1. **GitLab account.** Sign up at https://gitlab.com. Use the `dcdude71` name to keep your real name off the submission.
2. **Private commit email.** In GitLab: avatar → Edit profile → Emails. Note the `…@users.noreply.gitlab.com` address. Then Edit profile → Main settings → **Commit email**: pick that noreply address and save. Commits you make in the web editor then don't show your real email.
3. **CI pipelines (optional but helpful).** Your fork runs F-Droid's checks automatically. GitLab asks new accounts to verify their identity (phone or card) before it runs pipelines. If you skip this, the F-Droid reviewers run the checks for you; it is just slower.

## When v1.0.0 is tagged and released

Claude updates `io.github.dcdude71.castlist.yml` for 1.0.0 first (the `commit`, `versionName` and `versionCode` lines are already set for it).

1. **Fork fdroiddata.** Open https://gitlab.com/fdroid/fdroiddata and click **Fork** (top right). Keep the defaults.
2. **Create a branch.** In your fork: Code → Branches → **New branch**, name it `castlist`, from `master`.
3. **Add the recipe.** In your fork, on the `castlist` branch, open the `metadata` folder. Click **+** → **New file**.
   - File name: `io.github.dcdude71.castlist.yml`
   - Content: paste the whole file from this folder.
   - Commit message: `New app: Castlist`
   - Target branch: `castlist`. Click **Commit changes**.
4. **Open the merge request.** GitLab shows a "Create merge request" banner; click it, or go to Merge requests → New merge request, source `castlist` in your fork, target `fdroid/fdroiddata` `master`.
   - Title: `New app: Castlist`
   - Description: choose the **App inclusion** template if GitLab offers it, then add the text below under its checklist.
   - Tick **Allow commits from members who can merge**, so reviewers can fix small things themselves.
   - Click **Create merge request**.
5. **Wait and answer.** Reviewers usually respond within days to a few weeks. They may ask for changes in the recipe; edit the file on the `castlist` branch of your fork and the MR updates itself. Ask Claude to prepare any change they request.
6. **After the merge.** F-Droid builds and publishes the app within a few days. Later versions are picked up automatically from new `v…` tags, as long as `app/castlist-version.properties` is bumped before each tag.

## Text for the merge request

```
Castlist is a podcast player based on AntennaPod (de.danoeh.antennapod, already in F-Droid), with its own application id so it installs next to it. It adds rule-based playlists (with auto-download, a home screen widget, shortcuts and Android Auto), a condition-based episode filter, a per-podcast Skip ad button and a modern player layout.

- Source: https://github.com/dcdude71/Castlist (GPL-3.0, same license as AntennaPod)
- The free flavor is built, with the same Wear OS prebuild as AntennaPod's recipe.
- Version name and code come from app/castlist-version.properties (versionCode above AntennaPod's own scheme).
- Fastlane metadata is in fastlane/metadata/android/en-US.
- I am the developer of the fork. It is a separate app, not affiliated with the AntennaPod team, and the store description says so.
```

## Things to know

- **Signing.** F-Droid signs its build with its own key. A Castlist installed from GitHub or Obtainium can't be updated by F-Droid (and the reverse): switching means a database export, uninstalling, installing the other one, and importing.
- **Anti-features.** AntennaPod has none in F-Droid, and Castlist adds no tracking or non-free libraries, so none are declared. Reviewers may still add one if they disagree.
- **Android Auto.** Like every app not installed from the Play Store, Castlist from F-Droid needs Android Auto's developer setting "Unknown sources" to show up in the car (see docs/playlists.md).
