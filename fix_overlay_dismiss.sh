sed -i 's/currentErrorMessage = null/currentErrorMessage = null\n                                JarvisServiceState.setError(null)/g' app/src/main/java/com/example/services/JarvisService.kt
