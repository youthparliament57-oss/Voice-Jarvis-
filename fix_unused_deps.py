import re

with open("gradle/libs.versions.toml", "r") as f:
    lines = f.readlines()

new_lines = []
skip_words = [
    "room", "coil", "retrofit", "converter", "camera", "moshi", 
    "firebase", "credentials", "googleid", "tensorflow", 
    "roborazzi", "secrets", "google-services", "play-services-location",
    "ksp", "googleDevtoolsKsp"
]

for line in lines:
    skip = False
    for word in skip_words:
        if word in line.lower() or word in line:
            skip = True
            break
    if not skip:
        new_lines.append(line)

with open("gradle/libs.versions.toml", "w") as f:
    f.writelines(new_lines)
