# StormyQ NLP Models

This directory contains the Apache OpenNLP models required for enhanced text processing capabilities.

## Required Models

Download the following models from the [Apache OpenNLP Models](https://opennlp.apache.org/models.html) page:

### Core Models (Required)
1. **en-sent.bin** - English sentence detection model
   - Download: https://opennlp.sourceforge.net/models-1.5/en-sent.bin
   - Size: ~64KB

2. **en-token.bin** - English tokenization model
   - Download: https://opennlp.sourceforge.net/models-1.5/en-token.bin
   - Size: ~640KB

3. **en-pos-maxent.bin** - English POS tagging model
   - Download: https://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin
   - Size: ~3.2MB

### Named Entity Recognition Models (Required)
4. **en-ner-person.bin** - Person entity recognition
   - Download: https://opennlp.sourceforge.net/models-1.5/en-ner-person.bin
   - Size: ~3.8MB

5. **en-ner-location.bin** - Location entity recognition
   - Download: https://opennlp.sourceforge.net/models-1.5/en-ner-location.bin
   - Size: ~1.2MB

6. **en-ner-organization.bin** - Organization entity recognition
   - Download: https://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin
   - Size: ~2.1MB

7. **en-ner-date.bin** - Date entity recognition
   - Download: https://opennlp.sourceforge.net/models-1.5/en-ner-date.bin
   - Size: ~1.8MB

## Installation Instructions

1. Download all models listed above
2. Place them in this directory (`app/src/main/assets/models/`)
3. Ensure the files are named exactly as specified above
4. The total size will be approximately 12MB

## Model Loading

The models are automatically loaded by the `NLPPipeline` class during initialization. If any model fails to load, the application will fall back to enhanced heuristic methods.

## Performance Impact

- **Cold start**: First initialization may take 2-3 seconds
- **Memory usage**: Approximately 15-20MB additional RAM
- **Processing speed**: 50-200ms per sentence depending on complexity
- **Caching**: Processed results are cached for improved performance

## Optional Enhancements

For even better performance, consider these lightweight alternatives:

1. **Quantized models**: Use smaller, faster models if available
2. **Custom models**: Train domain-specific models for your use case
3. **Model compression**: Use compressed model formats to reduce size

## Troubleshooting

If models fail to load:
1. Check file names match exactly
2. Verify models are in the correct directory
3. Ensure sufficient device storage (>50MB free)
4. Check Android logs for specific error messages

The app will continue to function with basic text processing if models are unavailable.