# Voice Cleaning API — Mobile Integration Guide

**API version: 2.2.0**

This document matches the **BolSaaf Voice API v2.2** contract used by the mobile apps. It includes clean flow plus phase-2 reel features, adaptive hints, and quality-guard compatibility keys.

---

## Base URL

```
Production: https://shadowselfwork.com/voice/
```

All paths below are relative to the API root (`/voice/`). Trailing slashes are shown as implemented on the server.

---

## Endpoints

### 1. Health check

**`GET /voice/health/`**

Confirms the service is up. In v2, the response may include extra capability fields depending on deployment; clients should tolerate unknown keys.

**Example response (200):**

```json
{
  "status": "ok",
  "service": "BolSaaf Voice Cleaning API",
  "version": "2.0.0"
}
```

If `version` still shows an older string on the server, treat it as a deployment detail; **v2 behavior** is defined by the clean/status JSON shapes below.

---

### 2. Upload and clean audio

**`POST /voice/clean/`**

**`Content-Type:`** `multipart/form-data`

| Field  | Type   | Required | Default    | Description |
|--------|--------|----------|------------|-------------|
| `file` | file   | Yes      | —          | Audio: **MP3, M4A, WAV, OGG, FLAC** (max **5 MB**) |
| `mode` | string | No       | `standard` | `basic`, `standard`, `studio`, `pro` |

**Success (200)** — typical shape:

```json
{
  "status": "success",
  "message": "✨ Audio cleaned successfully",
  "job_id": 123,
  "cleaned_url": "/media/cleaned/clean_123_recording.wav",
  "duration": 10.5,
  "processing_time": 2.4,
  "mode": "standard"
}
```

- **`cleaned_url`** may be a **full URL** (`https://…`) or a **path** starting with `/media/…`. Mobile clients **must** resolve relative paths against the **site origin** (see [Resolving `cleaned_url`](#resolving-cleaned_url)).
- **`processing_time`** is server-side processing time in seconds (when provided).
- If the success payload omits **`cleaned_url`** but includes **`job_id`**, poll **`GET /voice/status/{job_id}/`** until `status` is `completed` or `failed` (see [Job status](#3-job-status)).

**Errors**

- **400** — e.g. file too large, invalid parameters:

```json
{
  "status": "error",
  "message": "File size exceeds 5MB limit"
}
```

- **500** — processing failure:

```json
{
  "status": "error",
  "message": "Processing failed: Invalid audio format"
}
```

---

### 3. Job status

**`GET /voice/status/{job_id}/`**

Use this for **async-style** flows or when the clean response did not include a downloadable URL yet.

**Completed (200):**

```json
{
  "job_id": 1,
  "status": "completed",
  "duration": 10.5,
  "processing_mode": "studio",
  "cleaned_url": "/media/cleaned/clean_1_recording.wav",
  "error_message": null,
  "created_at": "2026-04-07T10:30:00Z",
  "updated_at": "2026-04-07T10:30:05Z"
}
```

**Processing (200):**

```json
{
  "job_id": 2,
  "status": "processing",
  "duration": null,
  "processing_mode": "studio",
  "cleaned_url": null,
  "error_message": null,
  "created_at": "2026-04-07T10:35:00Z",
  "updated_at": "2026-04-07T10:35:01Z"
}
```

**Failed (200):**

```json
{
  "job_id": 3,
  "status": "failed",
  "duration": null,
  "processing_mode": "studio",
  "cleaned_url": null,
  "error_message": "Audio processing failed: Invalid audio format",
  "created_at": "2026-04-07T10:40:00Z",
  "updated_at": "2026-04-07T10:40:02Z"
}
```

**Not found (404):**

```json
{
  "error": "Job not found"
}
```

**Important:** Here, top-level **`status`** is the **job state** (`completed` | `processing` | `failed`), not the same as the clean endpoint’s `"status": "success"` wrapper.
For backward compatibility, the server may also include legacy keys like `state`, `mode`, and `processing_time`; clients should prefer `status` + `processing_mode`.

---
## 4) Phase-2+ endpoints used by mobile

The app now uses these async endpoints in addition to `/clean/`:

- `POST /voice/extract_voice/`
- `POST /voice/add_background/`
- `POST /voice/reel/` (**primary path**)
- `POST /voice/video/process/`
- `GET /voice/backgrounds/`

### Optional adaptive and quality metadata

Status payloads may include extra objects:

```json
{
  "adaptive": {
    "rms_dbfs": -62.4,
    "near_zero_fraction": 0.72,
    "confidence": 0.86,
    "preset": {
      "pre_gain": 4.0,
      "denoise_level": "STRONG",
      "compressor_strength": "MEDIUM",
      "dry_mix": 0.10,
      "mode": "studio"
    }
  },
  "quality_guard": {
    "issues": ["output_very_quiet"],
    "retry_applied": true,
    "dry_mix_applied": true,
    "loudness_floor_applied": true
  }
}
```

These keys are optional; clients should ignore unknown keys safely.

---

## Processing modes (v2.2)

| Mode        | Typical use |
|------------|-------------|
| `basic`    | Lightest pipeline; fastest |
| `standard` | Default; balanced quality and speed |
| `studio`   | Stronger processing |
| `pro`      | Maximum quality (slowest) |

Exact DSP/AI steps depend on server configuration.

### BolSaaf Android app mapping (reference)

| App preset   | `mode` sent to API |
|-------------|---------------------|
| Normal      | `standard`          |
| Strong      | `studio`            |
| Studio      | `pro`               |

---

## File requirements

| Requirement   | Value |
|---------------|--------|
| Max file size | **5 MB** |
| Formats       | **MP3, M4A, WAV, OGG, FLAC** (per v2 API) |
| Channels      | Mono or stereo (server may downmix) |
| Sample rate   | 48 kHz recommended for voice; server may resample |

**Mobile note:** The Android app may still export **WAV** before upload for a predictable pipeline; you may also upload supported formats directly if your product flow allows, staying under 5 MB.

---

## Resolving `cleaned_url`

Never pass a relative path straight to `URL` / `HttpURLConnection` without a host.

**JavaScript**

```javascript
function resolveCleanedUrl(voiceBaseUrl, cleanedUrl) {
  if (!cleanedUrl) return null;
  const s = String(cleanedUrl).trim();
  if (/^https?:\/\//i.test(s)) return s;
  const origin = new URL(voiceBaseUrl).origin;
  return s.startsWith('/') ? origin + s : `${origin}/${s}`;
}
```

**Kotlin (conceptual)**

```kotlin
fun resolveCleanedUrl(voiceBaseUrl: String, cleanedUrl: String): String {
    val u = cleanedUrl.trim()
    if (u.startsWith("http://", ignoreCase = true) || u.startsWith("https://", ignoreCase = true)) return u
    val origin = java.net.URL(voiceBaseUrl.trimEnd('/') + "/").let { "${it.protocol}://${it.host}${if (it.port == -1 || it.port == it.defaultPort) "" else ":${it.port}"}" }
    return if (u.startsWith("/")) origin + u else "$origin/$u"
}
```

The production app implements this in **`com.bolsaaf.audio.VoiceCleaningApi`**.

---

## Integration flow

### Recommended mobile flow

```
1. Record or pick audio
   ↓
2. Ensure file ≤ 5 MB (and format accepted by API)
   ↓
3. Show “Processing…”
   ↓
4. POST /voice/clean/ (multipart: file + mode)
   ↓
5. If cleaned_url is present → resolve if relative → GET download
   If cleaned_url missing but job_id present → poll GET /voice/status/{job_id}/
   ↓
6. Play or save cleaned file locally
```

- Use **background threads** / coroutines for upload, download, and polling.
- Use generous **read timeouts** for large uploads (e.g. 60–120 s).
- Poll every **1–2 s** with a **max wait** (e.g. 2 minutes) to avoid infinite loops.

---

## Mobile integration examples

Examples use production base URL `https://shadowselfwork.com/voice/`. Always resolve `cleaned_url` as in the previous section.

### React Native

```javascript
import FormData from 'form-data';

function resolveCleanedUrl(voiceBaseUrl, cleanedUrl) {
  if (!cleanedUrl) return null;
  const s = String(cleanedUrl).trim();
  if (/^https?:\/\//i.test(s)) return s;
  const origin = new URL(voiceBaseUrl).origin;
  return s.startsWith('/') ? origin + s : `${origin}/${s}`;
}

async function cleanAudio(audioFilePath, mode = 'standard') {
  const formData = new FormData();
  formData.append('file', {
    uri: audioFilePath,
    type: 'audio/wav',
    name: 'recording.wav',
  });
  formData.append('mode', mode);

  const response = await fetch('https://shadowselfwork.com/voice/clean/', {
    method: 'POST',
    body: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });

  const result = await response.json();
  if (result.status !== 'success') {
    console.error(result.message);
    return null;
  }

  let path = (result.cleaned_url || '').trim();
  if (!path && result.job_id) {
    path = await pollUntilCleaned(result.job_id);
  }
  const downloadUrl = resolveCleanedUrl('https://shadowselfwork.com/voice/', path);
  return { ...result, downloadUrl };
}

async function pollUntilCleaned(jobId) {
  const base = 'https://shadowselfwork.com/voice/status/';
  for (let i = 0; i < 80; i++) {
    if (i > 0) await new Promise((r) => setTimeout(r, 1500));
    const res = await fetch(`${base}${jobId}/`);
    const j = await res.json();
    if (j.status === 'completed' && j.cleaned_url) return j.cleaned_url;
    if (j.status === 'failed') throw new Error(j.error_message || 'Job failed');
  }
  throw new Error('Timeout waiting for job');
}

async function checkJobStatus(jobId) {
  const response = await fetch(`https://shadowselfwork.com/voice/status/${jobId}/`);
  if (response.status === 404) return null;
  return response.json();
}
```

### Flutter

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

String resolveCleanedUrl(String voiceBaseUrl, String? cleanedUrl) {
  if (cleanedUrl == null || cleanedUrl.isEmpty) return '';
  final s = cleanedUrl.trim();
  if (s.startsWith('http://') || s.startsWith('https://')) return s;
  final origin = Uri.parse(voiceBaseUrl).origin;
  return s.startsWith('/') ? '$origin$s' : '$origin/$s';
}

Future<Map<String, dynamic>?> cleanAudio(String audioFilePath, {String mode = 'standard'}) async {
  final request = http.MultipartRequest(
    'POST',
    Uri.parse('https://shadowselfwork.com/voice/clean/'),
  );
  request.files.add(await http.MultipartFile.fromPath(
    'file',
    audioFilePath,
    contentType: MediaType('audio', 'wav'),
  ));
  request.fields['mode'] = mode;

  final streamed = await request.send();
  final body = await streamed.stream.bytesToString();
  final result = jsonDecode(body) as Map<String, dynamic>;

  if (result['status'] != 'success') return null;

  var path = (result['cleaned_url'] ?? '').toString().trim();
  if (path.isEmpty && result['job_id'] != null) {
    path = await pollUntilCleaned(result['job_id'] as int);
  }
  result['downloadUrl'] = resolveCleanedUrl('https://shadowselfwork.com/voice/', path);
  return result;
}

Future<String> pollUntilCleaned(int jobId) async {
  final uri = Uri.parse('https://shadowselfwork.com/voice/status/$jobId/');
  for (var i = 0; i < 80; i++) {
    if (i > 0) await Future.delayed(const Duration(milliseconds: 1500));
    final res = await http.get(uri);
    final j = jsonDecode(res.body) as Map<String, dynamic>;
    if (j['status'] == 'completed' && (j['cleaned_url'] ?? '').toString().isNotEmpty) {
      return j['cleaned_url'].toString();
    }
    if (j['status'] == 'failed') {
      throw Exception(j['error_message'] ?? 'Job failed');
    }
  }
  throw Exception('Timeout');
}
```

### Swift (iOS)

```swift
import Foundation

func resolveCleanedUrl(voiceBaseUrl: String, cleanedUrl: String) -> String {
    let s = cleanedUrl.trimmingCharacters(in: .whitespacesAndNewlines)
    if s.lowercased().hasPrefix("http://") || s.lowercased().hasPrefix("https://") { return s }
    guard let u = URL(string: voiceBaseUrl) else { return s }
    var origin = "\(u.scheme ?? "https")://\(u.host ?? "")"
    if let p = u.port, p > 0, !(p == 80 && u.scheme == "http"), !(p == 443 && u.scheme == "https") {
        origin += ":\(p)"
    }
    return s.hasPrefix("/") ? "\(origin)\(s)" : "\(origin)/\(s)"
}

func cleanAudio(audioFileURL: URL, mode: String = "standard") async throws -> [String: Any] {
    let url = URL(string: "https://shadowselfwork.com/voice/clean/")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    let boundary = UUID().uuidString
    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

    var body = Data()
    let fileData = try Data(contentsOf: audioFileURL)
    body.append("--\(boundary)\r\n".data(using: .utf8)!)
    body.append("Content-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n".data(using: .utf8)!)
    body.append("Content-Type: audio/wav\r\n\r\n".data(using: .utf8)!)
    body.append(fileData)
    body.append("\r\n".data(using: .utf8)!)
    body.append("--\(boundary)\r\n".data(using: .utf8)!)
    body.append("Content-Disposition: form-data; name=\"mode\"\r\n\r\n".data(using: .utf8)!)
    body.append(mode.data(using: .utf8)!)
    body.append("\r\n".data(using: .utf8)!)
    body.append("--\(boundary)--\r\n".data(using: .utf8)!)
    request.httpBody = body

    let (data, _) = try await URLSession.shared.data(for: request)
    guard let result = try JSONSerialization.jsonObject(with: data) as? [String: Any],
          result["status"] as? String == "success" else {
        throw NSError(domain: "VoiceAPI", code: -1)
    }
    return result
}

func checkJobStatus(jobId: Int) async throws -> [String: Any] {
    let url = URL(string: "https://shadowselfwork.com/voice/status/\(jobId)/")!
    let (data, _) = try await URLSession.shared.data(from: url)
    return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
}
```

### Kotlin (Android)

Production code uses **`com.bolsaaf.audio.VoiceCleaningApi`**: it posts to **`/clean/`**, resolves relative **`cleaned_url`**, optionally polls **`/status/{job_id}/`**, parses **`processing_time`**, and downloads to a **`File`**.

Minimal OkHttp-style sketch (v2 shape):

```kotlin
// POST multipart to https://shadowselfwork.com/voice/clean/
// On success JSON: status == "success" → cleaned_url and/or job_id
// GET https://shadowselfwork.com/voice/status/{id}/ until status == "completed" or "failed"
// Download final URL after resolveCleanedUrl(baseVoiceUrl, cleaned_url)
```

---

## Error handling

### Common cases

| Situation | What to do |
|-----------|------------|
| `File size exceeds 5MB limit` | Shorten recording or re-encode |
| Invalid or unsupported format | Use an allowed format or transcode |
| `Job not found` (404) | Stale `job_id`; re-upload |
| Job `failed` with `error_message` | Show message; optional retry with another `mode` |

---

## Best practices

1. **Always resolve** relative `cleaned_url` before download.
2. **Poll** when `cleaned_url` is empty but `job_id` is present.
3. **Timeouts:** connect ~30 s, read ~120 s for upload+download; cap polling duration.
4. **Haptics / UI:** show indeterminate progress until download completes.
5. **Cache** cleaned files locally for replay and offline use.

---

## Testing

```bash
curl https://shadowselfwork.com/voice/health/

curl -X POST https://shadowselfwork.com/voice/clean/ \
  -F "file=@recording.wav" \
  -F "mode=standard"

curl https://shadowselfwork.com/voice/status/123/
```

---

## Support

- **`GET /voice/health/`** for availability
- Inspect JSON `message` / `error_message` fields
- Contact the backend team for persistent failures

---

## Version history

- **v2.2.0** (April 2026)
  - Reel-first mobile flow (`Reel ★` primary)
  - Optional adaptive/quality metadata in status payloads
  - Phase-2 endpoints aligned with mobile (`extract`, `bg`, `reel`, `video`)

- **v2.0.0** (April 2026)
  - Modes: `basic`, `standard`, `studio`, `pro` (default `standard`)
  - Multiformat uploads (MP3, M4A, WAV, OGG, FLAC)
  - `processing_time` on clean success
  - Job status API: `completed` | `processing` | `failed` with `cleaned_url` / `error_message`
  - Relative `cleaned_url` paths — clients must resolve to absolute URLs
  - Mobile integration doc aligned with app behavior (`VoiceCleaningApi`, preset → mode mapping)

- **v1.0.0**
  - Earlier contract (legacy); superseded by v2 for new integrations.
