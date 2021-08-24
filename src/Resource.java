import java.io.*;

class Resource {

	static private final String RESOURCE_DIR = "res";

	static private final String OWL_EXTN = "owl";
	static private final String XML_EXTN = "xml";

	static File owlFile(String name) {

		return resourceFile(name, OWL_EXTN);
	}

	static File xmlFile(String name) {

		return resourceFile(name, XML_EXTN);
	}

	static private File resourceFile(String name, String extn) {

		return new File(RESOURCE_DIR, name + '.' + extn);
	}
}
