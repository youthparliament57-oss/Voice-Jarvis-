cat << 'INNER_EOF' > fix.py
import sys

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "private fun resetSessionTimer()" in line:
        new_lines.append(line)
        new_lines.append("        sessionTimeoutJob?.cancel()\n")
        new_lines.append("        sessionTimeoutJob = serviceScope.launch {\n")
        new_lines.append("            delay(settingsRepository.getSessionTimeout())\n")
        new_lines.append("            Log.d(\"JarvisService\", \"Session timed out due to inactivity. Returning to Wake Word listening.\")\n")
        new_lines.append("            startWakeWordMode()\n")
        new_lines.append("        }\n")
        new_lines.append("    }\n")
        skip = True
        continue
        
    if skip:
        if "override fun onStartCommand" in line:
            skip = False
            new_lines.append(line)
        continue
        
    new_lines.append(line)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.writelines(new_lines)
INNER_EOF
python3 fix.py
