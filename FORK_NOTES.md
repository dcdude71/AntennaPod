# Fork notes

Handoff notes for working on this fork (on any machine, with or without Claude Code). Upstream `AGENTS.md` still applies.

## Repo setup
- Fork: `dcdude71/Castlist` (`origin`, renamed from `dcdude71/AntennaPod` in v0.9.3). Upstream: `AntennaPod/AntennaPod` (add it as `upstream`).
- All fork work is on `develop`, rebased on upstream `develop`. The old `fork-setup` and `playlists` branches are gone; their commits are on `develop`.
- App name: "Castlist" (debug: "Castlist Debug"), set in `app/build.gradle`. It was "AntennaPod Mix" up to v0.9.2. Release APKs are named `Castlist-<tag>.apk`.
- Application id: `io.github.dcdude71.castlist` (was `io.github.dcdude71.antennapodmix` up to v0.9.2), so it installs next to the official app. Icon sources: `art/castlist-icon/`.
- Releases: push a `v*` tag and `.github/workflows/fork-release.yml` builds and signs the APK. The keystore and passwords are GitHub secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), so nothing needs to be copied between machines. A tag with a dash (`v0.7.0-beta.1`) is published as a GitHub pre-release; Obtainium users get it only with "Include prereleases" on.
- If a tag push doesn't start the release build (happened with v0.7.1, published from a local build), push the tag anyway, then start it by hand: GitHub → Actions → Fork Release → Run workflow, entering the tag. Or from the command line: `gh workflow run fork-release.yml -R dcdude71/Castlist -f tag=v0.7.2`. A local build needs `-PforkVersionName` and the signing properties, exactly as in the workflow.
- `local.properties` and `.idea/` are machine-specific and not committed.
- Upstream's `Checks` and `Copilot Setup Steps` workflows are disabled on the fork (GitHub → Actions; their emulator tests expect upstream's UI). Before a release, run `./gradlew checkstyle lint` plus the module tests locally, and upstream's XML formatter on layouts: `find . -wholename "*/res/layout/*.xml" | xargs java -jar android-xml-formatter.jar` (1.1.0, from github.com/ByteHamster/android-xml-formatter); it must leave no diff.
- Commit as `dcdude71 <dcdude71@users.noreply.github.com>` (set with `git config user.name/user.email` in this repo), never under a real name.

## Keep the diff against upstream small
The fork is rebased on upstream regularly, so:
- Put new code in new, fork-only classes and files. Only add small hooks to upstream files.
- Never bump the database version. Fork tables are created with `CREATE TABLE IF NOT EXISTS` from `PodDBAdapter`'s helper `onOpen` (`PlaylistDbAdapter.createTables`, `FeedAdSkipDbAdapter.createTables`).
- `git diff upstream/develop...HEAD --stat` lists everything the fork touches.

## Rule-based playlists
Approved design (open to adjustment once used in practice):
- Playlists exist alongside the untouched queue. No master playlist, no manual "add episode to playlist", no per-podcast default playlist.
- Playlist = name + rules + auto-download + show in Android Auto.
- Rule = podcast, count (0 = all), pick from most recent / oldest, play order oldest / newest first, priority 1-10. Same priority: rule order (drag to reorder).
- "Most recent N" = the N newest episodes, only the unplayed ones among them (does not walk into the back catalog). "Oldest N" = the next N unplayed.
- The playlist is recalculated live after each episode. Playing from a playlist never adds to the queue; starting any other episode goes back to the normal queue.
- Playlist mode starts only from a toolbar play button (30dp icon, `ic_play_30dp`): "Play playlist" on a playlist, or "Play playlists from the top" on the Playlists tab (first playlist with episodes). Episode rows in a playlist have no play button (hidden via the hook `EpisodesListFragment.onEpisodeBound`); to play one episode, go to its podcast. Playing from the episode details page does not start playlist mode.
- At the end of a playlist: setting `prefPlaylistEndAction` (Settings → Playback), "Continue with the next playlist" (default, follows Playlists tab order, skips empty ones, stops after the last) or "Stop". Never falls back to the queue. Read via `PlaylistPreferences`.
- Playlist auto-downloads follow the normal download settings, never enqueue, and playlist episodes are protected from download cleanup.
- Playlist downloads do not count toward the global episode limit (`AutomaticDownloadAlgorithm`, `EpisodeCleanupAlgorithm.getNumEpisodesToCleanup`). To keep them bounded, rules in an auto-download playlist need a count of 1-10 (`PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES`); the rule dialog disables "All" and the auto-download toggle refuses while an "All" rule exists. `PlaylistReader.limitForAutoDownload` caps legacy rules anyway. The same cap applies to cleanup protection in every playlist: only the first 10 episodes per rule are protected and left out of the limit, so a playlist with an "All" rule cannot keep unlimited downloads.
- Fixed (also an upstream bug): when downloaded episodes exceeded the episode limit, `episodeSpaceLeft` went negative and `subList` threw inside an executor, silently skipping all auto-downloads. Now clamped to 0.
- Upstream clears an episode's auto-download flag after its first successful download (`MediaDownloadedHandler`) or a cancelled download, so a deleted episode is never auto-downloaded again. Playlist auto-download ignores that flag (it is false for every episode downloaded once, including in the official app before an import). Instead, episodes the user deletes or cancels by hand are recorded in the fork table `PlaylistDismissedEpisodes` (`PlaylistWriter.dismissEpisode`, one-line hooks in `DeleteActionButton`, `DeleteSwipeAction`, `FeedItemMenuHandler`, `EpisodeMultiSelectActionHandler`, `CancelDownloadActionButton`) and never auto-downloaded by playlists. Cleanup and delete-after-playing are not recorded.
- All three cleanup algorithms protect playlist episodes (`APCleanupAlgorithm` only deletes played ones, which playlists never hold). Leaving the rule editor (`PlaylistEditFragment.onStop`) starts auto-download.
- Code: `model/.../playlist/`, `storage/database/.../Playlist*` (DbAdapter, Engine, Reader, Writer, Playback), `app/.../ui/screen/playlist/`. Tests in `storage/database/src/test/.../Playlist*Test.java`.

Phases:
1. Data model and engine: done
2. Screens and rule editor: done
3. Playback: done
4. Auto-download and cleanup protection: done
5. Android Auto: built, not yet tested in a car or the Desktop Head Unit. A "Playlists" folder appears after "Continue listening" when at least one playlist has "Show in Android Auto" ticked (playlist screen ⋮ menu; on by default for new playlists, off for existing ones). Starting an episode from it starts playlist playback. Code: `PlaylistMediaLibrary` plus small hooks in `MediaLibrarySessionCallback`.

Inspired by Downcast's playlist engine.

## Testing on a device
- Debug build: `./gradlew --console=plain :app:installPlayDebug`, then `adb shell monkey -p io.github.dcdude71.castlist.debug -c android.intent.category.LAUNCHER 1` (AGENTS.md's launch command uses the upstream package name and does not work here).
- The debug app is separate from the release app. Import a database export to test with real podcasts. Downloaded files from the release app are not readable by the debug app, so reset `FeedMedia.downloaded`/`file_url` in the imported copy (`adb exec-out run-as io.github.dcdude71.castlist.debug cat databases/Antennapod.db`, edit with sqlite3, push back with `run-as ... sh -c 'cat > databases/Antennapod.db'` while the app is force-stopped).

## Skip ad
Per-podcast "Ad skip time" in the podcast's settings (fork table via `FeedAdSkip` / `FeedAdSkipDbAdapter`). When set, the player shows a "Skip ad" button that jumps ahead by that amount from the current position, separate from fast-forward.

## Player style
Settings → User interface → Player style: Modern (default) or Classic (upstream layout). Modern aims for an Apple Podcasts-like look: small artwork, bold title, big controls. Fork-only files: `PlayerStyle.java`, `PlayerLayout.java`, `audioplayer_fragment_modern.xml`.
- Foldables (Modern only, `PlayerLayout`): window ≥ 600dp wide → shownotes in `descriptionPane` beside the controls (pager shows the cover only, Shownotes button hidden). Half-open with a horizontal fold (tabletop, via `androidx.window`) → cover above the fold, controls below; overrides the wide layout. MainActivity handles size changes itself (no recreate), so the layout is re-applied from `AudioPlayerFragment.onConfigurationChanged`.
- Upstream `NestedScrollableHost` was changed so it works outside a ViewPager2 and no longer cancels its first frame (cancelling froze the unfold animation: `BLASTSyncEngine` sync timeout).
- Testing needs the real phone: folded, unfolded, half-folded and flat again.

## Playlists on wide screens
- `PlaylistsFragment` (layout `playlist_panes.xml`): ≥ 600dp wide → list (360dp) + selected playlist (`PlaylistEpisodesFragment` as a child in `detailPane`). Selection is remembered in prefs (`PlaylistPanes`).
- Pencil opens `PlaylistEditFragment` (always, also when narrow): `PlaylistRulesFragment` on the left, `PlaylistEpisodesFragment.newPreviewInstance` on the right when wide. Preview tints newly added episodes for 2 s. Narrow: rules toolbar subtitle "Preview: N episodes".
- Fold with two panes → selected playlist full screen (Back = list). A full-screen playlist that becomes visible on a wide screen (unfold, or Back from the editor) pops itself so the two panes show it.
- All fork-only files; no upstream changes.

## Episode details pane (wide screens)
- `EpisodeDetailPane` (fork-only) wraps `main_content_view` at runtime in a horizontal LinearLayout with a right-hand `episodeDetailPane` container (ids in `values/ids_fork.xml`), so upstream layout files stay untouched. It mirrors the main view's bottom margin (mini player).
- Hooks in `MainActivity`: `install` after `setContentView`, `show` at the top of `loadChildFragment` (intercepts `ItemPagerFragment` when ≥ 600dp), `close` in `loadFragment` (tab switch), `onConfigurationChanged` (folding moves the episode to full screen).
- In the pane the toolbar's back arrow becomes ✕; Back closes the pane (its callback is re-registered on open so it wins over MainActivity's).
- Known edge: `ItemFragment` pops the activity back stack if the episode no longer exists; in the pane that also steps the list back once.

## Working preferences
- Ask design questions as a short numbered list with a recommendation per item, and let the answer come back as plain text (not a multiple-choice picker); answers often add nuance the options don't cover.
