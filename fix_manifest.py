import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

content = re.sub(r'    <!-- Usage Stats for Proactive Context \(CRITICAL for Settings Usage Access List\) -->\n    <uses-permission\n        android:name="android.permission.PACKAGE_USAGE_STATS"\n        tools:ignore="ProtectedPermissions" />\n\n', '', content)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
