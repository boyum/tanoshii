#!/usr/bin/env python3
"""
Speech-to-Text service using faster-whisper.
Accepts audio file path as argument, outputs transcribed Japanese text.
"""

import sys
import os
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

def transcribe(audio_path: str) -> str:
    """Transcribe audio file to Japanese text."""
    model = get_model()

    # Transcribe with Japanese language hint
    segments, info = model.transcribe(
        audio_path,
        language="ja",
        beam_size=5,
        vad_filter=True,  # Filter out silence
    )

    # Combine all segments
    text = " ".join(segment.text.strip() for segment in segments)
    return text.strip()

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
