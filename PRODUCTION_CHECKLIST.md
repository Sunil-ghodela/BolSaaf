# BolSaaf Production Checklist

Pre-flight list before promoting a build to **Play Store Production**. Work top → bottom; each row is something a human (or Claude) can verify in <2 min.

> Last revised: 2026-04-15. Update when the rollout flow changes (new SKU, new sub-domain, new permission, etc.).

---

## 1. Code gates (run from repo root)

| ✅ | Gate | Command / file | Pass criteria |
| -- | ---- | -------------- | ------------- |
| ☐ | Unit tests green | `./gradlew :app:testDebugUnitTest` | exit 0 |
| ☐ | Release builds clean | `./gradlew :app:bundleRelease` | AAB present at `app/build/outputs/bundle/release/app-release.aab`, ≤ 25 MB |
| ☐ | Lint clean (release) | `./gradlew :app:lintRelease` | no `error` severity |
| ☐ | versionCode bumped | `app/build.gradle.kts` | strictly greater than the last code in Play Console |
| ☐ | versionName updated | `app/build.gradle.kts` | semver — bump `MINOR` for features, `PATCH` for fixes |
| ☐ | No `Log.d` / debug toggles in release path | `grep -RIn "Log\\.d\\|TODO_REMOVE\\|isDebuggable" app/src/main` | only intentional ones |
| ☐ | `.env` not committed with prod secrets | `git diff HEAD~1 -- .env` | empty / harmless |
| ☐ | Keystore + alias env vars present | `.env` has `SIGNING_KEYSTORE`, `SIGNING_ALIAS`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD` | resolves locally |
| ☐ | RNNoise native lib present in AAB | `unzip -l app-release.aab | grep librnnoise-lib.so` | one entry per ABI you target |
| ☐ | ProGuard / R8 didn't strip JNI bridge | manual: install AAB via `bundletool`, hit Live screen | mic capture works |

## 2. Manifest + permissions

| ✅ | Item | File | Pass criteria |
| -- | ---- | ---- | ------------- |
| ☐ | `RECORD_AUDIO` declared | `app/src/main/AndroidManifest.xml` | yes, with rationale shown in Play listing |
| ☐ | `CAMERA` declared (CameraX live) | same | yes |
| ☐ | `INTERNET` + `ACCESS_NETWORK_STATE` | same | yes |
| ☐ | No `READ_EXTERNAL_STORAGE` on SDK 33+ | same | uses scoped storage / SAF instead |
| ☐ | `targetSdk = 34` | `app/build.gradle.kts` | required by Play after Aug 2025 |
| ☐ | `minSdk = 24` | same | matches Play data-safety form |
| ☐ | App icon + adaptive icon present | `app/src/main/res/mipmap-*` | foreground + background layers |

## 3. Backend (server: `root@77.237.234.45`)

| ✅ | Check | Command | Pass criteria |
| -- | ----- | ------- | ------------- |
| ☐ | Voice API reachable | `curl -s -o /dev/null -w "%{http_code}\n" https://shadowselfwork.com/voice/health/` | 200 |
| ☐ | Privacy URL live | `curl -s -o /dev/null -w "%{http_code}\n" https://shadowselfwork.com/voice/privacy` | 200 |
| ☐ | Terms URL live | `curl -s -o /dev/null -w "%{http_code}\n" https://shadowselfwork.com/voice/terms` | 200 |
| ☐ | Bare `/privacy` + `/terms` still 200 | same | older AABs in the wild keep working |
| ☐ | Auth endpoints up | `curl -s -o /dev/null -w "%{http_code}\n" https://shadowselfwork.com/voice/auth/password-reset/confirm/` | 200 (serves reset form) |
| ☐ | Voice admin reachable | `curl -s -o /dev/null -w "%{http_code}\n" https://shadowselfwork.com/voice/admin/` | 302 (redirects to login) |
| ☐ | nginx config has no fresh warnings | `ssh root@... 'nginx -t'` | "syntax is ok / test is successful" |
| ☐ | `voice-gunicorn` socket up | `ssh root@... 'systemctl is-active voice-gunicorn.socket voice-gunicorn.service'` | both `active` |
| ☐ | `fast-music-remover` container up | `ssh root@... 'docker ps --filter name=fast-music-remover --format "{{.Status}}"'` | `Up …` |
| ☐ | Disk free > 5 GB on `/var/www` | `ssh root@... 'df -h /var/www'` | comfortably above |
| ☐ | DRF throttles still applied | `apps/voice/views.py` has `throttle_classes = [ScopedRateThrottle]` on every endpoint | yes |
| ☐ | Latest backups exist | `ssh root@... 'ls -1t /etc/nginx/sites-enabled/selfshadowwork.bak.* | head -1; ls -1t /var/www/simplelms/backend/apps/voice/tasks.py.bak.* | head -1'` | dates within last 30 days |

## 4. App ↔ server contract

| ✅ | Check | Where | Pass criteria |
| -- | ----- | ----- | ------------- |
| ☐ | Status JSON parser handles legacy keys | `VoiceApiPhase2Client.kt` | both `status`+`processing_mode` and `state`+`mode` parsed |
| ☐ | `cleaned_url` resolution covers relative URLs | `VoiceCleaningApi.kt` | resolves against site origin |
| ☐ | `friendlyHttpError` mapped 401/403/413/429/5xx | `VoiceApiPhase2Client.kt` (bottom) | yes — visible toasts in app |
| ☐ | FastLib client size caps match server `client_max_body_size` | `MainActivity.FASTLIB_MAX_*` (25 MB / 50 MB) ↔ nginx `voice_upload` location | match |
| ☐ | `VOICE_API_CONTRACT_PHASE2_PLUS.md` matches code | doc ↔ both clients | no drift |

## 5. UX smoke (manual, on a real device)

Run all of these on the connected device (`./gradlew :app:installDebug` first if you want hot iteration):

| ✅ | Flow | Pass criteria |
| -- | ---- | ------------- |
| ☐ | Cold launch | < 2 s to Home, no crash, splash → Home |
| ☐ | Quick clean (audio file, ≤ 5 MB) | banner → Cleaning → Recent Cleans card with waveform + Compare |
| ☐ | Quick clean (large audio, > 25 MB) | rejected with friendly toast, no upload |
| ☐ | Video clean (≤ 50 MB MP4) | progress dialog flips Cleaning → Downloading %, video card opens external player |
| ☐ | Make Reel banner | tap → Reel sheet → success card |
| ☐ | Add Vibe → BG selector | bottom sheet first, then file picker |
| ☐ | Live recording (audio) | record → stop → clean → playback OK |
| ☐ | Live recording (video) | CameraX preview, finalize plays back |
| ☐ | History tab | scrollable, share + delete work |
| ☐ | Profile → Plan modal | "View plans" opens modal, Free vs Pro shown, "Upgrade to Pro" toasts |
| ☐ | Settings → Privacy Policy | opens `https://shadowselfwork.com/voice/privacy` in browser |
| ☐ | Settings → Terms of Service | opens `/voice/terms` |
| ☐ | Free quota chip | counts down per minute consumed, flips red at ≤ 3, alert at 0 |
| ☐ | FastLib lab | upload audio → 3-step progress → output card with Play / Share / Save |
| ☐ | Login / logout | dialog accepts email+password, profile reflects state, logout clears |
| ☐ | Rotate device on Home | no crash, no double picker |
| ☐ | Airplane mode mid-upload | friendly error, no crash |
| ☐ | Permission denial (mic) | rationale shown, no crash |

## 6. Play Console listing

| ✅ | Field | Value / source |
| -- | ----- | -------------- |
| ☐ | App name | `BolSaaf` |
| ☐ | Short description | one liner from `IMPLEMENTATION_SUMMARY.md` |
| ☐ | Full description | full pitch — voice cleaner + reel engine |
| ☐ | App category | Music & Audio (alt: Video Players & Editors) |
| ☐ | Content rating | Everyone |
| ☐ | Privacy Policy URL | `https://shadowselfwork.com/voice/privacy` |
| ☐ | Terms of Service URL | `https://shadowselfwork.com/voice/terms` |
| ☐ | Support email | `ss.sunil9255@gmail.com` |
| ☐ | App icon (512×512 PNG) | brand red→purple→blue gradient mark |
| ☐ | Feature graphic (1024×500) | hero with brand gradient + "Studio jaisi awaaz" |
| ☐ | Phone screenshots × 4–8 | Home, Make Reel, Live, Plan modal, FastLib |
| ☐ | Promo video (optional) | 30 s YouTube link |
| ☐ | Data safety form filled | mic / camera / network + declare Google Mobile Ads SDK (reads advertising ID on Android 13-) |
| ☐ | Ads declaration | **Yes — AdMob** (`ca-app-pub-9194903827759003~6689280046`). SDK wired, no ad units placed yet. |
| ☐ | Target audience | 13+ |
| ☐ | Content guidelines self-cert | done |
| ☐ | Countries / regions | Worldwide (or staged rollout) |
| ☐ | Pricing | Free; in-app product = none yet (toast only) |

## 7. Release-track promotion

| ✅ | Step | Notes |
| -- | ---- | ----- |
| ☐ | Upload AAB to **Internal testing** | first dogfood track |
| ☐ | Add yourself + 1 other tester | email or Google Group |
| ☐ | Install via Internal testing link on a **fresh** device | not your dev device |
| ☐ | Confirm Play Signing on first upload | accept Google's signing key |
| ☐ | Smoke section 5 again on the Play-installed AAB | catches R8 / shrinking issues |
| ☐ | Promote to **Closed testing** (optional) | share with ~10 users for a week |
| ☐ | Promote to **Production** (staged rollout) | start at 10 % → 50 % → 100 % over 7 days |
| ☐ | Pre-launch report green | Play scans on real devices; address any crash before 100 % |
| ☐ | App content questionnaire reviewed | data-safety, ads, target audience |

## 8. Post-release monitoring (first 72 h)

| ✅ | Watch | Where | Trigger |
| -- | ----- | ----- | ------- |
| ☐ | Crash-free rate ≥ 99 % | Play Console → Android vitals | rollback if < 98 % |
| ☐ | ANRs ≤ 0.47 % | same | investigate spikes |
| ☐ | 4xx / 5xx rate on `/voice/clean/` | server `nginx access.log` (or future Grafana) | normal levels |
| ☐ | `429`s spiking? | DRF / nginx logs | tune throttle limits if legitimate |
| ☐ | Disk pressure on `/var/www/simplelms/backend/media/cleaned/` | `df -h` + cron cleanup | add purge job if needed |
| ☐ | User reviews / ratings | Play Console | reply within 24 h |
| ☐ | Support email | `ss.sunil9255@gmail.com` | triage daily |

## 9. Known gaps to close before next release

- **Billing**: Pro CTA toasts; wire Google Play Billing if monetizing.
- **Crash reporting**: no Crashlytics / Sentry yet — add before 1 k installs.
- **Analytics**: no event pipeline — add only with explicit data-safety disclosure.
- **i18n**: app is English + Hinglish copy hardcoded — extract to `strings.xml` before adding more locales.
- **Dark theme**: scheme defined in `MD3Theme.kt` but `BolSaafTheme(darkTheme = false)` forced. Re-enable system dark mode opt-in once dark variants of brand assets exist.
- **Token refresh / auth**: login is email/password stub — no real auth backend.

## 10. Roll-back plan

If crash-free drops below 98 % or `/voice/` 5xx rate doubles after release:

1. **Halt rollout** in Play Console (Production → Manage rollout → Halt).
2. **Server**: revert `nginx` config to most recent `selfshadowwork.bak.*` and `systemctl reload nginx`. Check `tasks.py.bak.*` if a backend deploy went out the same day.
3. **Decide**: hot-fix release (bump `versionCode`, fix, re-bundle, internal → 10 % → ramp) **or** restore previous version (Play Console → Production → Resume rollout of older release).
4. **Communicate**: in-app banner (next release) + reply to affected reviews.

---

**Checklist owner**: Vaibhav. Update this file when you discover a new pre-flight gate the hard way — every "we should have caught that" goes here so the next ship is one step safer.
