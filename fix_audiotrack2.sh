sed -i 's/trackToRelease.flush()/trackToRelease.flush()\n                    trackToRelease.release()/g' app/src/main/java/com/example/gemini/GeminiLiveClient.kt
