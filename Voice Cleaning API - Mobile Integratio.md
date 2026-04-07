# Voice Cleaning API - Mobile Integration Guide

## Base URL
```
Production: https://shadowselfwork.com/voice/
```

---

## Endpoints

### 1. Health Check
**Endpoint:** `GET /voice/health/`

Check if the voice cleaning service is running.

**Request:**
```http
GET /voice/health/
```

**Response:**
```json
{
  "status": "ok",
  "service": "BolSaaf Voice Cleaning API",
  "version": "1.0.0"
}
```

**Status Code:** `200 OK`

---

### 2. Upload & Clean Audio
**Endpoint:** `POST /voice/clean/`

Upload an audio file and receive a cleaned version with noise reduction and audio enhancement.

**Request:**
```http
POST /voice/clean/
Content-Type: multipart/form-data
```

**Form Data:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | Audio file (WAV format, max 5MB) |
| mode | String | No | Processing mode: `basic`, `standard`, `studio` (default: `standard`) |

**Processing Modes:**
- **basic**: RNNoise denoising only (fastest)
- **standard**: RNNoise + EQ + compression (recommended)
- **studio**: Full processing with normalization (best quality)

**Response (Success):**
```json
{
  "status": "success",
  "job_id": 123,
  "cleaned_url": "https://shadowselfwork.com/media/cleaned/clean_123.wav",
  "duration": 10.5,
  "mode": "standard"
}
```

**Response (Error):**
```json
{
  "status": "error",
  "message": "File size exceeds 5MB limit"
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Invalid file or parameters
- `500 Internal Server Error` - Processing failed

---

### 3. Check Job Status
**Endpoint:** `GET /voice/status/{job_id}/`

Check the status of an audio processing job.

**Request:**
```http
GET /voice/status/123/
```

**Response (Success):**
```json
{
  "status": "success",
  "job_id": 123,
  "state": "completed",
  "cleaned_url": "https://shadowselfwork.com/media/cleaned/clean_123.wav",
  "duration": 10.5,
  "created_at": "2026-04-07T09:30:00Z"
}
```

**Response (Processing):**
```json
{
  "status": "success",
  "job_id": 123,
  "state": "processing",
  "progress": 50
}
```

**Response (Not Found):**
```json
{
  "error": "Job not found"
}
```

**Status Codes:**
- `200 OK` - Success
- `404 Not Found` - Job ID not found

---

## File Requirements

### Audio Format
- **Format**: WAV only
- **Sample Rate**: 48kHz recommended (auto-resampled if different)
- **Channels**: Mono or stereo (converted to mono for processing)
- **Max Size**: 5MB
- **Max Duration**: ~5 minutes (depending on quality)

### Why WAV?
- Lossless format
- Better processing quality
- RNNoise optimized for WAV

---

## Mobile Integration Examples

### React Native

```javascript
import FormData from 'form-data';
import fs from 'fs';

// Upload and clean audio
async function cleanAudio(audioFilePath, mode = 'standard') {
  const formData = new FormData();
  formData.append('file', {
    uri: audioFilePath,
    type: 'audio/wav',
    name: 'recording.wav',
  });
  formData.append('mode', mode);

  try {
    const response = await fetch('https://shadowselfwork.com/voice/clean/', {
      method: 'POST',
      body: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    const result = await response.json();
    
    if (result.status === 'success') {
      console.log('Cleaned audio URL:', result.cleaned_url);
      console.log('Duration:', result.duration, 'seconds');
      return result;
    } else {
      console.error('Error:', result.message);
      return null;
    }
  } catch (error) {
    console.error('Upload failed:', error);
    return null;
  }
}

// Check job status
async function checkJobStatus(jobId) {
  try {
    const response = await fetch(`https://shadowselfwork.com/voice/status/${jobId}/`);
    const result = await response.json();
    
    if (result.status === 'success') {
      console.log('Job state:', result.state);
      if (result.state === 'completed') {
        console.log('Download URL:', result.cleaned_url);
      }
      return result;
    } else {
      console.error('Error:', result.error);
      return null;
    }
  } catch (error) {
    console.error('Status check failed:', error);
    return null;
  }
}
```

### Flutter

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

// Upload and clean audio
Future<Map<String, dynamic>?> cleanAudio(
  String audioFilePath, 
  String mode = 'standard'
) async {
  var request = http.MultipartRequest(
    'POST',
    Uri.parse('https://shadowselfwork.com/voice/clean/'),
  );

  request.files.add(await http.MultipartFile.fromPath(
    'file',
    audioFilePath,
    contentType: MediaType('audio', 'wav'),
  ));
  request.fields['mode'] = mode;

  try {
    var response = await request.send();
    var responseBody = await response.stream.bytesToString();
    var result = jsonDecode(responseBody);

    if (result['status'] == 'success') {
      print('Cleaned audio URL: ${result['cleaned_url']}');
      print('Duration: ${result['duration']} seconds');
      return result;
    } else {
      print('Error: ${result['message']}');
      return null;
    }
  } catch (error) {
    print('Upload failed: $error');
    return null;
  }
}

// Check job status
Future<Map<String, dynamic>?> checkJobStatus(int jobId) async {
  try {
    var response = await http.get(
      Uri.parse('https://shadowselfwork.com/voice/status/$jobId/'),
    );
    var result = jsonDecode(response.body);

    if (result['status'] == 'success') {
      print('Job state: ${result['state']}');
      if (result['state'] == 'completed') {
        print('Download URL: ${result['cleaned_url']}');
      }
      return result;
    } else {
      print('Error: ${result['error']}');
      return null;
    }
  } catch (error) {
    print('Status check failed: $error');
    return null;
  }
}
```

### Swift (iOS)

```swift
import Foundation

// Upload and clean audio
func cleanAudio(audioFileURL: URL, mode: String = "standard") async throws -> [String: Any] {
    let url = URL(string: "https://shadowselfwork.com/voice/clean/")!
    
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    
    let boundary = UUID().uuidString
    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
    
    var body = Data()
    
    // Add file
    let fileData = try Data(contentsOf: audioFileURL)
    body.append("--\(boundary)\r\n".data(using: .utf8)!)
    body.append("Content-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n".data(using: .utf8)!)
    body.append("Content-Type: audio/wav\r\n\r\n".data(using: .utf8)!)
    body.append(fileData)
    body.append("\r\n".data(using: .utf8)!)
    
    // Add mode
    body.append("--\(boundary)\r\n".data(using: .utf8)!)
    body.append("Content-Disposition: form-data; name=\"mode\"\r\n\r\n".data(using: .utf8)!)
    body.append(mode.data(using: .utf8)!)
    body.append("\r\n".data(using: .utf8)!)
    
    body.append("--\(boundary)--\r\n".data(using: .utf8)!)
    
    request.httpBody = body
    
    let (data, _) = try await URLSession.shared.data(for: request)
    
    if let result = try JSONSerialization.jsonObject(with: data) as? [String: Any],
       result["status"] as? String == "success" {
        print("Cleaned audio URL: \(result["cleaned_url"] ?? "")")
        print("Duration: \(result["duration"] ?? 0) seconds")
        return result
    } else {
        throw NSError(domain: "VoiceAPI", code: -1, userInfo: ["message": "Upload failed"])
    }
}

// Check job status
func checkJobStatus(jobId: Int) async throws -> [String: Any] {
    let url = URL(string: "https://shadowselfwork.com/voice/status/\(jobId)/")!
    
    let (data, _) = try await URLSession.shared.data(from: url)
    
    if let result = try JSONSerialization.jsonObject(with: data) as? [String: Any],
       result["status"] as? String == "success" {
        print("Job state: \(result["state"] ?? "")")
        if result["state"] as? String == "completed" {
            print("Download URL: \(result["cleaned_url"] ?? "")")
        }
        return result
    } else {
        throw NSError(domain: "VoiceAPI", code: -1, userInfo: ["message": "Status check failed"])
    }
}
```

### Kotlin (Android)

```kotlin
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Upload and clean audio
suspend fun cleanAudio(
    audioFile: File,
    mode: String = "standard"
): JSONObject? = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            audioFile.name,
            audioFile.asRequestBody("audio/wav".toMediaType())
        )
        .addFormDataPart("mode", mode)
        .build()
    
    val request = Request.Builder()
        .url("https://shadowselfwork.com/voice/clean/")
        .post(requestBody)
        .build()
    
    try {
        val response = client.newCall(request).execute()
        val result = JSONObject(response.body?.string() ?: "")
        
        if (result.getString("status") == "success") {
            println("Cleaned audio URL: ${result.getString("cleaned_url")}")
            println("Duration: ${result.getDouble("duration")} seconds")
            return@withContext result
        } else {
            println("Error: ${result.getString("message")}")
            return@withContext null
        }
    } catch (e: Exception) {
        println("Upload failed: ${e.message}")
        return@withContext null
    }
}

// Check job status
suspend fun checkJobStatus(jobId: Int): JSONObject? = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    
    val request = Request.Builder()
        .url("https://shadowselfwork.com/voice/status/$jobId/")
        .get()
        .build()
    
    try {
        val response = client.newCall(request).execute()
        val result = JSONObject(response.body?.string() ?: "")
        
        if (result.getString("status") == "success") {
            println("Job state: ${result.getString("state")}")
            if (result.getString("state") == "completed") {
                println("Download URL: ${result.getString("cleaned_url")}")
            }
            return@withContext result
        } else {
            println("Error: ${result.getString("error")}")
            return@withContext null
        }
    } catch (e: Exception) {
        println("Status check failed: ${e.message}")
        return@withContext null
    }
}
```

---

## Integration Flow

### Recommended Flow for Mobile App

```
1. User Records Audio
   ↓
2. Convert to WAV format (if needed)
   ↓
3. Show "Processing..." UI
   ↓
4. Upload to /voice/clean/
   ↓
5. Receive cleaned_url immediately (synchronous)
   ↓
6. Download and play cleaned audio
```

**Note:** Current implementation is synchronous. For large files or heavy load, consider:
- Showing loading spinner during upload
- Implementing timeout handling (30-60 seconds)
- Adding retry logic for failed uploads

---

## Error Handling

### Common Errors

| Error | Description | Solution |
|-------|-------------|----------|
| `File size exceeds 5MB limit` | Audio file too large | Compress or reduce recording duration |
| `Invalid file format` | Not a WAV file | Convert to WAV before upload |
| `Processing failed` | Server error during processing | Retry with different mode or contact support |
| `Job not found` | Invalid job ID | Check job ID or upload again |

### Error Response Format

```json
{
  "status": "error",
  "message": "Error description here"
}
```

---

## Best Practices

### 1. File Preparation
- Convert recordings to WAV format before upload
- Ensure sample rate is 48kHz for best quality
- Keep file size under 5MB
- Test with sample files before production

### 2. User Experience
- Show progress indicator during upload
- Display estimated processing time (5-10 seconds for 1-minute audio)
- Provide "Cancel" option for long uploads
- Cache cleaned audio locally for replay

### 3. Error Handling
- Implement retry logic (max 3 attempts)
- Show user-friendly error messages
- Log errors for debugging
- Provide fallback option (upload without cleaning)

### 4. Performance
- Compress audio before upload if possible
- Use background threads for upload
- Implement timeout (60 seconds)
- Monitor API response times

---

## Testing

### Test with Sample Audio

```bash
# Test health check
curl https://shadowselfwork.com/voice/health/

# Test upload (replace with your WAV file)
curl -X POST \
  https://shadowselfwork.com/voice/clean/ \
  -F "file=@recording.wav" \
  -F "mode=standard"

# Test job status
curl https://shadowselfwork.com/voice/status/123/
```

---

## Support

For issues or questions:
- Check server health: `GET /voice/health/`
- Review error messages in response
- Contact development team for persistent issues

---

## Version History

- **v1.0.0** (April 2026)
  - Initial release
  - RNNoise denoising
  - EQ and compression
  - Loudness normalization
  - Three processing modes
