import re

with open("build.gradle.kts", "r") as f:
    content = f.read()

content = re.sub(r'    alias\(libs\.plugins\.google\.devtools\.ksp\) apply false\n', '', content)
content = re.sub(r'    alias\(libs\.plugins\.roborazzi\) apply false\n', '', content)
content = re.sub(r'    alias\(libs\.plugins\.secrets\) apply false\n', '', content)
content = re.sub(r'    alias\(libs\.plugins\.google\.services\) apply false\n', '', content)

with open("build.gradle.kts", "w") as f:
    f.write(content)
