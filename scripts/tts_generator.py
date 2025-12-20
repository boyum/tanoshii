#!/usr/bin/env python3
"""
Edge TTS wrapper script for generating Japanese audio.
Requires: pip install edge-tts
"""

import argparse
import asyncio
import edge_tts


async def generate_audio(text: str, voice: str, output: str):
    """Generate audio using Edge TTS."""
    communicate = edge_tts.Communicate(text, voice)
    await communicate.save(output)


def main():
    parser = argparse.ArgumentParser(description='Generate TTS audio using Edge TTS')
    parser.add_argument('--text', required=True, help='Text to convert to speech')
    parser.add_argument('--voice', default='ja-JP-NanamiNeural', help='Voice to use')
    parser.add_argument('--output', required=True, help='Output file path')

    args = parser.parse_args()
    asyncio.run(generate_audio(args.text, args.voice, args.output))


if __name__ == '__main__':
    main()
