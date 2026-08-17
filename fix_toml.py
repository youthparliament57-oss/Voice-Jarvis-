import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

# Versions to remove
versions_to_remove = [
    'junit = "4.13.2"',
    'junitVersion = "1.3.0"',
    'espressoCore = "3.7.0"',
    'kotlinxCoroutinesTest = "1.10.2"',
    'core = "1.6.1"',
    'runner = "1.6.2"',
    'kotlinxCoroutinesAndroid = "1.10.2"',
    'kotlinxCoroutinesCore = "1.10.2"',
    'playServicesLocation = "21.3.0"',
    'loggingInterceptor = "4.10.0"',
    'robolectric = "4.16.1"',
    'googleServices = "4.5.0"'
]

for v in versions_to_remove:
    content = content.replace(f"{v}\n", "")

# Libraries to remove
libs_to_remove = [
    r'junit = \{ group = "junit", name = "junit", version\.ref = "junit" \}\n',
    r'androidx-junit = \{ group = "androidx\.test\.ext", name = "junit", version\.ref = "junitVersion" \}\n',
    r'androidx-espresso-core = \{ group = "androidx\.test\.espresso", name = "espresso-core", version\.ref = "espressoCore" \}\n',
    r'kotlinx-coroutines-test = \{ group = "org\.jetbrains\.kotlinx", name = "kotlinx-coroutines-test", version\.ref = "kotlinxCoroutinesTest" \}\n',
    r'androidx-core = \{ group = "androidx\.test", name = "core", version\.ref = "core" \}\n',
    r'androidx-runner = \{ group = "androidx\.test", name = "runner", version\.ref = "runner" \}\n',
    r'kotlinx-coroutines-android = \{ group = "org\.jetbrains\.kotlinx", name = "kotlinx-coroutines-android", version\.ref = "kotlinxCoroutinesAndroid" \}\n',
    r'kotlinx-coroutines-core = \{ group = "org\.jetbrains\.kotlinx", name = "kotlinx-coroutines-core", version\.ref = "kotlinxCoroutinesCore" \}\n',
    r'logging-interceptor = \{ group = "com\.squareup\.okhttp3", name = "logging-interceptor", version\.ref = "loggingInterceptor" \}\n',
    r'robolectric = \{ group = "org\.robolectric", name = "robolectric", version\.ref = "robolectric" \}\n'
]

for l in libs_to_remove:
    content = re.sub(l, "", content)

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
