# JARVIS Assistant - Phase 1 Architecture & Goals

## Goal
Build a proactive voice assistant that can Listen → Understand → Act → Observe → Learn → Suggest.

## Core Capabilities
1. **Wake Word Engine**: Reliable wake-word detection (e.g., "JARVIS") with continuous listening mode and session management.
2. **Professional Voice Pipeline**: STT (Hindi/English/Hinglish) -> LLM -> TTS with low latency.
3. **Continuous Conversation Manager**: State management (IDLE, AWAKE, LISTENING, UNDERSTANDING, EXECUTING, SPEAKING).
4. **LLM Brain**: Intent extraction and tool selection (not direct Android control).
5. **Intent Engine**: Predefined catalog (System, Apps, Communication, Web, Media, Information).
6. **Tool/Action Layer**: Router -> Tool Registry -> Executor -> Android API.
7. **Android Native Integration**: Use Intents, App Links, APIs (no Accessibility/Shizuku in Phase 1).
8. **Proactive Intelligence Engine**: Signal Collection -> Pattern Detection -> Relevance Scoring -> Suggestion.
9. **Usage Activity Collector**: Track app usage via UsageStatsManager.
10. **Event Memory**: Explicit memory and Behavioural memory.
11. **Memory Architecture**: Room/SQLite for initial phases.
12. **Pattern Detection**: Statistical analysis for routine detection.
13. **Proactive Decision Engine**: Score-based decision to speak or stay silent.
14. **Proactive Categories**: Routines, Reminders, Contextual, Suggestions.
15. **Health Integration**: Basic Health Connect signals (privacy-conscious).
16. **Proactive Scheduler**: Android-compliant event/time-based triggers.
17. **Voice Personality Layer**: LLM -> Formatter -> TTS persona.
18. **Interruptibility**: Ability to stop TTS instantly.
19. **Latency Target**: Streaming-first architecture for fast response.
20. **Permissions**: Progressive request model.
21. **Safety/Confirmation Layer**: Risk classification before execution.
22. **Observability**: Detailed logging for every action step.

## Phase 1 Architecture Diagram
```
┌─────────────────────┐
│     MICROPHONE      │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│  AUDIO PROCESSING   │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│    WAKE WORD        │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ CONVERSATION ENGINE │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│        STT          │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│    LLM BRAIN        │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│   ACTION ROUTER     │
└──────────┬──────────┘
           ↓
┌──────────┼──────────┐
↓          ↓          ↓
Android   Web       App
APIs      APIs      Intents
│          │          │
└──────────┼──────────┘
           ↓
┌─────────────────────┐
│   ACTION RESULT     │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│   RESPONSE LAYER    │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│        TTS          │
└──────────┬──────────┘
           ↓
     USER HEARS
```

## Out of Scope for Phase 1
- Accessibility Service
- Shizuku
- Screen understanding
- Multi-agent system
- Complex workflow automation
- Deep OS modification
