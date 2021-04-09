package io.vertigo.ai.plugins.nlu.rasa;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.vertigo.ai.impl.nlu.NluEnginePlugin;
import io.vertigo.ai.impl.nlu.NluManagerImpl;
import io.vertigo.ai.nlu.VIntent;
import io.vertigo.ai.nlu.VIntentClassification;
import io.vertigo.ai.nlu.VRecognitionResult;
import io.vertigo.ai.plugins.nlu.rasa.mda.ConfigFile;
import io.vertigo.ai.plugins.nlu.rasa.mda.MessageToRecognize;
import io.vertigo.ai.plugins.nlu.rasa.mda.RasaIntentNlu;
import io.vertigo.ai.plugins.nlu.rasa.mda.RasaIntentWithConfidence;
import io.vertigo.ai.plugins.nlu.rasa.mda.RasaParsingResponse;
import io.vertigo.ai.plugins.nlu.rasa.util.FileIOUtil;
import io.vertigo.ai.plugins.nlu.rasa.util.RasaHttpSenderUtil;
import io.vertigo.core.lang.Assertion;
import io.vertigo.core.param.ParamValue;
import io.vertigo.core.resource.ResourceManager;

public class RasaNluEnginePlugin implements NluEnginePlugin {

	private final String name;
	private final String rasaUrl;

	private final URL configFileUrl;

	private Boolean ready;

	@Inject
	public RasaNluEnginePlugin(
			@ParamValue("rasaUrl") final String rasaUrl,
			@ParamValue("configFile") final Optional<String> configFileOpt,
			@ParamValue("pluginName") final Optional<String> pluginNameOpt,
			final ResourceManager resourceManager) {

		Assertion.check().isNotBlank(rasaUrl);

		this.rasaUrl = rasaUrl;
		name = pluginNameOpt.orElse(NluManagerImpl.DEFAULT_ENGINE_NAME);

		final var configFileName = configFileOpt.orElse("rasa-config.yaml"); // in classpath by default
		Assertion.check().isNotBlank(configFileName);

		configFileUrl = resourceManager.resolve(configFileName);

		ready = false;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void train(final Map<VIntent, List<String>> trainingData) {
		//Yaml file
		final ConfigFile config = FileIOUtil.getConfigFile(configFileUrl);
		final List<RasaIntentNlu> intents = createRasaIntents(trainingData);

		//Create map and launch the dump
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("language", config.getLanguage());
		map.put("pipeline", config.getPipeline());
		map.put("nlu", intents);

		//train
		final String filename = RasaHttpSenderUtil.launchTraining(rasaUrl, map);

		ready = false;

		//put model
		RasaHttpSenderUtil.putModel(rasaUrl, filename);

		ready = true;
	}

	private static List<RasaIntentNlu> createRasaIntents(final Map<VIntent, List<String>> trainingData) {
		final List<RasaIntentNlu> result = new ArrayList<>();
		for (final Entry<VIntent, List<String>> entry : trainingData.entrySet()) {
			final RasaIntentNlu intent = new RasaIntentNlu(entry.getKey().getCode(), createTrainingSetence(entry.getValue()));
			result.add(intent);
		}
		return result;
	}

	private static String createTrainingSetence(final List<String> value) {
		if (value.isEmpty()) {
			return "";
		}
		// output yaml format for Rasa
		return "- " + String.join("\n- ", value);
	}

	/** {@inheritDoc} */
	@Override
	public VRecognitionResult recognize(final String sentence) {
		int retry = 0;
		while (!ready && retry < 5) {
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt(); //si interrupt on relance
			}
			retry++;
		}

		if (!ready) {
			throw new IllegalStateException("NLU engine '" + getName() + "' is not ready to recognize sentenses.");
		}

		final MessageToRecognize message = new MessageToRecognize(sentence);
		final RasaParsingResponse response = RasaHttpSenderUtil.getIntentFromRasa(rasaUrl, message);

		return getVRecognitionResult(response);
	}

	private VRecognitionResult getVRecognitionResult(final RasaParsingResponse response) {
		final String rawSentence = response.getIntent().getName();
		final List<VIntentClassification> intentClassificationList = response.getIntent_ranking().stream()
				.map(this::getVIntentFromFromRasaIntent)
				.collect(Collectors.toList());
		return new VRecognitionResult(rawSentence, intentClassificationList);
	}

	private VIntentClassification getVIntentFromFromRasaIntent(final RasaIntentWithConfidence rasaIntent) {
		return new VIntentClassification(VIntent.of(rasaIntent.getName()), rasaIntent.getConfidence());
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isReady() {
		return ready;
	}
}
