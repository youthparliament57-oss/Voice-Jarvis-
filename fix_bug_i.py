import re

with open("app/src/main/java/com/example/audio/AudioController.kt", "r") as f:
    content = f.read()

pattern = r"if \(rms > VAD_THRESHOLD\) \{\s+consecutiveSpeechFrames\+\+\s+if \(consecutiveSpeechFrames == 3\) \{\s+_userSpeakingFlow\.tryEmit\(true\)\s+\}\s+\} else \{\s+consecutiveSpeechFrames = 0\s+\}"

replacement = """if (rms > VAD_THRESHOLD) {
                                    consecutiveSpeechFrames++
                                    if (consecutiveSpeechFrames == 3) {
                                        _userSpeakingFlow.tryEmit(true)
                                    }
                                } else {
                                    if (consecutiveSpeechFrames >= 3) {
                                        _userSpeakingFlow.tryEmit(false)
                                    }
                                    consecutiveSpeechFrames = 0
                                }"""

content = re.sub(pattern, replacement, content)

with open("app/src/main/java/com/example/audio/AudioController.kt", "w") as f:
    f.write(content)
