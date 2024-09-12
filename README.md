# Generate Subtitles for Long Videos (in English, Java)

This project is an audio transcription service leveraging Cloudflare Workers AI and the OpenAI Whisper model.
It showcases the extraction of audio from video files and the transcribing of speech into text using an asynchronous approach.
The project manipulates and processes audios in parallel with AI-powered transcription service, constructing into a common subtitle format (.srt).

**The project is designed to handle multiple **large** video/audio files efficiently by breaking them into segments and processing them concurrently with Java.**

## Usage

1. Put all videos in a single folder
2. Replace `FOLDER` value with the video folder(in step 1) path in `Util.java`.
3. Run `SubtitleGenerator.Main`.
4. Srt file will be generated in same folder(in step 1) with same file name as its corresponding video.
5. Check log info that has `Complete with (0) ...`, the number in parentheses indicates how many `segments` Whisper AI failed to transcribe. A video has `video_length_in_seconds/MAX_SEGMENT_DURATION_SECONDS` segments.

## Key Features

- **Producer-Consumer Design**: 
  * (Deprecated)Optimized the Thread Pool and Blocking Queue to implement a producer-consumer design with thread-safety, ensuring an efficient and stable workflow.
- **Multithreaded Implementation**: 
  * Utilizes multithreading structure to operate audio segments and make multiple transcription requests in parallel in order to ensure efficiency and likely ended with at least **15x** speedups.
- **Semaphore Design**:
  * Applied semaphore to ensure a handled workflow arrangement with great success rate in multi-threaded environment.
- **Audio Extraction**:
  * Extracts audio from video files and prepares them for transcription.
- **Handling multiple Files**:
  * Processing all videos in one folder simultaneously, get rid of serial processing.
- **Handling Large Files**: 
  * Automatically splits audio files into smaller segments (e.g., 1-minute chunks) to avoid API size limitations.
- **Subtitle Generation**:
  * Combines transcriptions of each audio segment into a single `.srt` subtitle file.
- **Cloudflare Workers Integration**:
  * Leverages Cloudflare Workers AI for secure and scalable serverless processing of API requests.
- **OpenAI Whisper (Speech-to-Text) model Integration**:
  * Brings AI integrations with Speech-to-Text power.

## Project Highlights

- **Multi-Large Videos**:
  * Capable of handling multiple, large video/audio files simultaneously.
- **Trained Performance**:
  * Tuned auto retry mechanism, semaphore and threads to ensure high efficiency, accuracy, and stability.
- **Efficiency**:
  * Uses multithreading and concurrency features in Java to improve the efficiency of API requests and overall processing speed.
- **API Integration**:
  * Integrates with Cloudflare's OpenAI Whisper API for high-quality speech-to-text transcription.

## Installation
### Prerequisites

- Java 11 or higher
- Maven (for dependency management)
- Cloudflare account with access to OpenAI Whisper API
- FFmpeg (for audio extraction)

### Setup

1. Clone this repository:
    ```bash
    git clone https://github.com/yourusername/yourproject.git
    cd yourproject
    ```
2. Install dependencies:
    ```bash
   mvn install
    ```
3. Make sure **`ffmpeg`** is installed and available in your system's PATH.
4. Replace placeholders for **`CF_ACCOUNT_ID`** and **`"CF_API_TOKEN"`** in Util.java with your Cloudflare access.

## Technical Challenges
This project tackled several technical challenges, including:

* **Handling Large Files**: Splitting large audio files into smaller chunks for processing due to API constraints.
* **Concurrency**: Optimized performance through the use of CompletableFuture for asynchronous processing.
* **SSL/TLS Integration**: Properly managing secure API requests with SSL/TLS and handling errors such as bad_record_mac.
* **OpenAI Whisper AI Model Integration**: Integrating properly with OpenAI's Whisper-tiny-en model.