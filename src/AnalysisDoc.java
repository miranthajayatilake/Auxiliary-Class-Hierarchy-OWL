import java.io.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.w3c.dom.ls.*;

/**
 * @author Colin Puleston
 */
class AnalysisDoc {

	private final File OUT_FILE = Resource.xmlFile("analysis");

	private final String ROOT_TAG = "BirdsOntologyAnalysis";
	private final String BIRD_PAIR_TAG = "BirdPairAnalysis";

	private final String BIRD_A_ATTR = "bird_A";
	private final String BIRD_B_ATTR = "bird_B";
	private final String LOCAL_SIMILARITY_ATTR = "localSimilarity";
	private final String STRUCTURAL_SIMILARITY_ATTR = "structuralSimilarity";
	private final String STEPS_ATTR = "steps";

	private final String PRETTY_PRINT_ID = "format-pretty-print";

	private Document document;
	private Element rootElement;

	AnalysisDoc() {

		document = createBuilder().newDocument();
		rootElement = document.createElement(ROOT_TAG);

		document.appendChild(rootElement);
	}

	void addPairAnalysis(String birdA, String birdB, Float localSimilarity, Float structuralSimilarity, Integer steps) {

		Element pathElement = document.createElement(BIRD_PAIR_TAG);

		rootElement.appendChild(pathElement);

		pathElement.setAttribute(BIRD_A_ATTR, birdA);
		pathElement.setAttribute(BIRD_B_ATTR, birdB);
		pathElement.setAttribute(LOCAL_SIMILARITY_ATTR, localSimilarity.toString());
		pathElement.setAttribute(STRUCTURAL_SIMILARITY_ATTR, structuralSimilarity.toString());
		pathElement.setAttribute(STEPS_ATTR, steps.toString());
	}

	void writeToFile() {

		FileOutputStream output = openOutputStream();

		try {

			writeToOutputStream(output);
		}
		catch (IOException e) {

			closeOutputStream(output);

			throw new RuntimeException(e);
		}

		closeOutputStream(output);
	}

	private DocumentBuilder createBuilder() {

		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			factory.setNamespaceAware(true);

			return factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {

			throw new RuntimeException(e);
		}
	}

	private void writeToOutputStream(OutputStream output) throws IOException {

		DOMImplementationLS impl = getImplementation();
		LSSerializer serializer = impl.createLSSerializer();
		LSOutput lsOutput = impl.createLSOutput();

		setPrettyPrint(serializer);
		lsOutput.setByteStream(output);
		serializer.write(document, lsOutput);
	}

	private FileOutputStream openOutputStream() {

		try {

			return new FileOutputStream(OUT_FILE);
		}
		catch (FileNotFoundException e) {

			throw new RuntimeException(e);
		}
	}

	private void closeOutputStream(OutputStream output) {

		try {

			output.close();
		}
		catch (IOException e) {

			throw new RuntimeException(e);
		}
	}

	private void setPrettyPrint(LSSerializer serializer) {

		DOMConfiguration config = serializer.getDomConfig();

		if (config.canSetParameter(PRETTY_PRINT_ID, true)) {

			config.setParameter(PRETTY_PRINT_ID, true);
		}
	}

	private DOMImplementationLS getImplementation() {

		return (DOMImplementationLS)document.getImplementation();
	}
}