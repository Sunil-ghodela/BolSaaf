# ReelVoice - Voice Cleaner App

Studio jaisa sound, ghar baithhe - AI-powered noise removal for your voice recordings.

## Tech Stack
- **NDK**: Native Development Kit for C/C++ integration
- **RNNoise**: Neural network based noise suppression
- **JNI**: Java Native Interface bridge
- **Kotlin**: Android development
- **Jetpack Compose**: Modern UI toolkit

## Architecture Flow
```
RNNoise Setup → NDK Build → JNI Bridge → Kotlin Wrapper → UI
```

## Project Structure
```
app/src/main/
├── cpp/                    # Native code
│   ├── CMakeLists.txt      # NDK build configuration
│   ├── rnnoise_jni.cpp     # JNI bridge
│   └── rnnoise/            # RNNoise library
│       ├── include/
│       └── src/
├── java/com/reelvoice/
│   ├── MainActivity.kt     # Main UI
│   └── audio/
│       ├── RNNoiseBridge.kt    # JNI wrapper
│       ├── AudioProcessor.kt   # Audio processing
│       └── AudioRecorder.kt    # Recording functionality
└── res/                    # Resources
```

## Build Instructions

1. **Setup Android Studio** with NDK and CMake
2. **Build the project**:
   ```bash
   ./gradlew build
   ```
3. **Run on device**:
   ```bash
   ./gradlew installDebug
   ```

## Features
- 🎙️ Real-time noise removal
- 📁 File upload (Audio/Video)
- 📦 Batch processing (Pro)
- 📊 Noise removal analytics
- 💾 WAV output format

## Permissions Required
- `RECORD_AUDIO` - For recording
- `READ_EXTERNAL_STORAGE` - For file upload
- `WRITE_EXTERNAL_STORAGE` - For saving output

## License
MIT License
