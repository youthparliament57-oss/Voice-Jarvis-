sed -i 's/delay(1000)//g' app/src/main/java/com/example/gemini/GeminiLiveClient.kt
sed -i 's/trackToRelease.stop()/trackToRelease.pause()\n                    trackToRelease.flush()\n                    trackToRelease.release()/g' app/src/main/java/com/example/gemini/GeminiLiveClient.kt
sed -i '/trackToRelease.release()/d' app/src/main/java/com/example/gemini/GeminiLiveClient.kt
