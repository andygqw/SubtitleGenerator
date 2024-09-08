# Generate Subtitles for Long Videos (in English, Java)

This project demonstrates the development of an audio transcription service leveraging Cloudflare Workers AI and the OpenAI Whisper model.
It showcases the extraction of audio from video files and the transcribing of speech into text using an asynchronous approach.
The project splits audio files into smaller segments, sends them in parallel to an AI-powered transcription service, and combines the results into a readable subtitle format (.srt).

The project is designed to handle **large** video/audio files efficiently by breaking them into segments and processing them concurrently with Java.

## Key Features

- **Handling Multiple Videos Simultaneously**: 
  * Designed for multiple video/audio manipulation asynchronously
- **Producer-Consumer Design**: 
  * Optimized the Thread Pool and Blocking Queue to implement a producer-consumer design with thread-safety, ensuring an efficient and stable workflow.
- **Multithreaded Implementation**: 
  * Utilizes multithreading structure to operate audio segments and make multiple transcription requests in parallel in order to ensure efficiency and likely ended with at least **15x** speedups.
- **Audio Extraction**:
  * Extracts audio from video files and prepares them for transcription.
- **Handling Large Files**: 
  * Automatically splits audio files into smaller segments (e.g., 1-minute chunks) to avoid API size limitations.
- **Subtitle Generation**:
  * Combines transcriptions of each audio segment into a single `.srt` subtitle file.
- **Cloudflare Workers Integration**:
  * Leverages Cloudflare Workers AI for secure and scalable serverless processing of API requests.
- **OpenAI Whisper (Speech-to-Text) model Integration**:
  * Brings AI integrations with Speech-to-Text power.

## Project Highlights

- **Modern Java Features**:
  * The project demonstrates usage of Java's `HttpClient`, `CompletableFuture`, and SSL/TLS handling.
- **API Integration**:
  * Integrates with Cloudflare's OpenAI Whisper API for high-quality speech-to-text transcription.
- **Efficiency**:
  * Uses multithreading and concurrency features in Java to improve the efficiency of API requests and overall processing speed.

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
4. Replace placeholders for **`CF_ACCOUNT_ID`** and **`"CF_API_TOKEN"`** in Extractor.java with your Cloudflare access.

## Technical Challenges
This project tackled several technical challenges, including:

* **Handling Large Files**: Splitting large audio files into smaller chunks for processing due to API constraints.
* **Concurrency**: Optimized performance through the use of CompletableFuture for asynchronous processing.
* **SSL/TLS Integration**: Properly managing secure API requests with SSL/TLS and handling errors such as bad_record_mac.
* **OpenAI Whisper AI Model Integration**: Integrating properly with OpenAI's Whisper-tiny-en model.