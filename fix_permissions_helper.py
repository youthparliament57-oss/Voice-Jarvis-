import re

with open("app/src/main/java/com/example/utils/PermissionsHelper.kt", "r") as f:
    content = f.read()

content = re.sub(r'    fun hasUsageStatsPermission\(context: Context\): Boolean \{.*?\n        return mode == AppOpsManager\.MODE_ALLOWED\n    }', '', content, flags=re.DOTALL)
content = re.sub(r'    @androidx\.annotation\.RequiresApi\(Build\.VERSION_CODES\.Q\)\n    private fun checkOpQ\(appOps: AppOpsManager, context: Context\): Int \{.*?\n    \}', '', content, flags=re.DOTALL)
content = re.sub(r'    /\*\*.*?\*/\n    fun getUsageStatsPermissionIntent\(context: Context\): Intent \{.*?\n    \}', '', content, flags=re.DOTALL)
content = re.sub(r'import android.app.AppOpsManager', '', content)
content = re.sub(r'import android.os.Process', '', content)

with open("app/src/main/java/com/example/utils/PermissionsHelper.kt", "w") as f:
    f.write(content)
