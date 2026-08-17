sed -i '/OutlinedButton/,/}/d' app/src/main/java/com/example/ui/screens/PermissionsScreen.kt
sed -i 's/onAllPermissionsGranted()//g' app/src/main/java/com/example/ui/screens/PermissionsScreen.kt
sed -i 's/if (allCoreGranted) {/if (allCoreGranted) {\n                            onAllPermissionsGranted()\n/g' app/src/main/java/com/example/ui/screens/PermissionsScreen.kt
