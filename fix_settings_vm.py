import re

with open("app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt", "r") as f:
    content = f.read()

# Add new flows
new_flows = """
    val systemPrompt: StateFlow<String> = repository.systemPromptFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        
    val modelName: StateFlow<String> = repository.modelNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        
    val wakeThreshold: StateFlow<Float> = repository.wakeThresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)
        
    val sessionTimeout: StateFlow<Long> = repository.sessionTimeoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120_000L)
"""

content = content.replace("    private val _validationState", new_flows + "\n    private val _validationState")

# Add save functions
save_funcs = """
    fun saveSystemPrompt(prompt: String) {
        viewModelScope.launch { repository.saveSystemPrompt(prompt) }
    }
    
    fun saveModelName(name: String) {
        viewModelScope.launch { repository.saveModelName(name) }
    }
    
    fun saveWakeThreshold(threshold: Float) {
        viewModelScope.launch { repository.saveWakeThreshold(threshold) }
    }
    
    fun saveSessionTimeout(timeoutMs: Long) {
        viewModelScope.launch { repository.saveSessionTimeout(timeoutMs) }
    }
"""

content = content.replace("    fun clearValidationState() {", save_funcs + "\n    fun clearValidationState() {")

with open("app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt", "w") as f:
    f.write(content)
