#!/usr/bin/env python3
"""
Speech-to-Text service using faster-whisper.
Accepts audio file path as argument, outputs transcribed Japanese text.
"""

import sys
import os
import subprocess
import tempfile
from faster_whisper import WhisperModel

# Use small model for balance of speed and accuracy
# Options: tiny, base, small, medium, large-v2, large-v3
MODEL_SIZE = os.environ.get("WHISPER_MODEL", "small")

# Cache the model globally for reuse
_model = None

def get_model():
    global _model
    if _model is None:
        # Use int8 quantization for faster inference on CPU
        # Use "cuda" and "float16" if you have a GPU
        _model = WhisperModel(MODEL_SIZE, device="cpu", compute_type="int8")
    return _model

def convert_to_wav(input_path: str) -> str:
    """Convert audio file to WAV format using ffmpeg."""
    # Create a temp file for the converted audio
    fd, wav_path = tempfile.mkstemp(suffix=".wav")
    os.close(fd)

    try:
        # Use ffmpeg to convert to 16kHz mono WAV (optimal for Whisper)
        result = subprocess.run(
            [
                "ffmpeg", "-y", "-i", input_path,
                "-ar", "16000",  # 16kHz sample rate
                "-ac", "1",      # mono
                "-c:a", "pcm_s16le",  # 16-bit PCM
                wav_path
            ],
            capture_output=True,
            text=True
        )

        if result.returncode != 0:
            raise RuntimeError(f"ffmpeg conversion failed: {result.stderr}")

        return wav_path
    except FileNotFoundError:
        raise RuntimeError("ffmpeg not found. Please install ffmpeg: brew install ffmpeg")

def transcribe(audio_path: str) -> str:
    """Transcribe audio file to Japanese text."""
    model = get_model()

    # Convert webm/other formats to wav for better compatibility
    wav_path = None
    try:
        if audio_path.endswith(".webm") or audio_path.endswith(".ogg"):
            wav_path = convert_to_wav(audio_path)
            audio_to_transcribe = wav_path
        else:
            audio_to_transcribe = audio_path

        # Transcribe with Japanese language hint
        segments, info = model.transcribe(
            audio_to_transcribe,
            language="ja",
            beam_size=5,
            vad_filter=True,  # Filter out silence
        )

        # Combine all segments
        text = " ".join(segment.text.strip() for segment in segments)
        return text.strip()
    finally:
        # Clean up temp wav file
        if wav_path and os.path.exists(wav_path):
            os.remove(wav_path)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python stt_service.py <audio_file_path>", file=sys.stderr)
        sys.exit(1)

    audio_path = sys.argv[1]

    if not os.path.exists(audio_path):
        print(f"Error: File not found: {audio_path}", file=sys.stderr)
        sys.exit(1)

    try:
        result = transcribe(audio_path)
        print(result)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
