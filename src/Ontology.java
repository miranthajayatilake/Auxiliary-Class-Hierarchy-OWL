import java.io.*;
import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.functional.renderer.OWLFunctionalSyntaxRenderer;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

class Ontology {

	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private OWLReasoner reasoner;

	Ontology(File inFile) {

		manager = OWLManager.createOWLOntologyManager();
		ontology = load(inFile);
		factory = manager.getOWLDataFactory();
		reasoner = createReasoner();
	}

	void save(File outFile) {

		try {

			PrintWriter writer = new PrintWriter(new FileWriter(outFile));

			try {

				new OWLFunctionalSyntaxRenderer().render(ontology, writer);
			}
			finally {

				writer.close();
			}
		}
		catch (OWLRendererException e) {

			throw new RuntimeException(e);
		}
		catch (IOException e) {

			throw new RuntimeException(e);
		}
	}

	void reclassify() {

		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.values());
	}

	OWLClass addClass(IRI iri) {

		OWLClass cls = getClass(iri);

		addAxiom(factory.getOWLDeclarationAxiom(cls));

		return cls;
	}

	void addEquivalence(OWLClassExpression eq1, OWLClassExpression eq2) {

		addAxiom(factory.getOWLEquivalentClassesAxiom(eq1, eq2));
	}

	OWLObjectIntersectionOf getIntersection(Set<OWLClassExpression> ops) {

		return factory.getOWLObjectIntersectionOf(ops);
	}

	Set<OWLClassAxiom> getAxiomsFor(OWLClass cls) {

		return ontology.getAxioms(cls, Imports.INCLUDED);
	}

	Set<OWLClass> getDirectSupers(OWLClassExpression expr) {

		return reasoner.getSuperClasses(expr, true).getFlattened();
	}

	Set<OWLClass> getDirectSubs(OWLClassExpression expr) {

		return reasoner.getSubClasses(expr, true).getFlattened();
	}

	OWLClass getClass(IRI iri) {

		return factory.getOWLClass(iri);
	}

	String getName(OWLEntity entity) {

		return entity.getIRI().getShortForm();
	}

	private OWLOntology load(File file) {

		try {

			return manager.loadOntologyFromOntologyDocument(file);
		}
		catch (OWLOntologyCreationException e) {

			throw new RuntimeException(e);
		}
	}

	private OWLReasoner createReasoner() {

		return new FaCTPlusPlusReasonerFactory().createReasoner(ontology);
	}

	private void addAxiom(OWLAxiom axiom) {

		manager.addAxiom(ontology, axiom);
	}
}
