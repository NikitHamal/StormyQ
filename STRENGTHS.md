# StormyQ - New Cognitive Capabilities

StormyQ has been upgraded from a basic word-matcher to a multi-layered cognitive engine.

## 1. Syntactic Intelligence (SVO)
The AI no longer just looks for words; it looks for **roles**. 
- **Syntactic Parser**: A custom pivot-based engine identifies Subjects, Verbs, and Objects.
- **Role-Based Activation**: Relationships in the Semantic Network are now typed (e.g., `ACTION`, `OBJECT`). This prevents confusion in sentences like "The man bit the dog" vs "The dog bit the man."

## 2. The Global Brain (Persistence)
StormyQ now possesses a long-term memory.
- **Knowledge Storage**: All learned rules and conceptual relations are serialized and saved to internal storage.
- **Persistent Growth**: Every high-confidence answer "trains" the AI, and this training persists even after the app is closed, allowing the model to get smarter over time.

## 3. Bayesian Probabilistic Inference
We have replaced simple scoring heuristics with a mathematical **Inference Engine**.
- **Evidence Weighting**: The AI calculates the *probability* of an answer being correct using Bayesian logic.
- **Multi-Factor Correlation**: It weighs Keyword Density, Role Alignment, Temporal Overlap, and Sentiment Consistency to arrive at a final posterior probability.

## 4. Enhanced NLP Pipeline
- **Porter Stemmer**: Robust linguistic stemming.
- **Sentiment & Temporal Modules**: Dedicated processors for emotional and time-based context.
- **Asynchronous Execution**: All cognitive tasks run off the main thread for a fluid user experience.

---
*StormyQ is now an advanced, offline, and self-improving QnA system.*
