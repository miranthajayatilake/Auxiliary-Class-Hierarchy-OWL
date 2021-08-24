import java.io.*;
import java.util.*;

import org.semanticweb.owlapi.model.*;

public class BirdyThing {

	static private final String OUT_FILE_SUFFIX = "-extended";

	static private final String NAMESPACE = "http://www.semanticweb.org/mirantha/ontologies/2021/2/untitled-ontology-99#";
	static private final String BIRD_GROUP_DEFN_NAME_SEPARATOR = "-AND-";

	static private final IRI BIRD_ROOT_CLS = toIRI("Bird");

	static private final int FLOAT_PRECISION = 100;

	static public void main(String[] args) {

		new BirdyThing(args[0]);
	}

	static private File owlFile(String name) {

		return Resource.owlFile(name);
	}

	static private IRI toIRI(String fragment) {

		return IRI.create(NAMESPACE + fragment);
	}

	private Ontology ontology;

	private OWLClass birdRootCls;

	private List<BirdProfile> birdProfiles = new ArrayList<BirdProfile>();
	private List<BirdPair> birdPairs = new ArrayList<BirdPair>();
	private List<BirdGroup> birdGroups = new ArrayList<BirdGroup>();

	private class BirdProfile {

		final OWLClass birdCls;

		final Set<OWLQuantifiedObjectRestriction> characters
					= new HashSet<OWLQuantifiedObjectRestriction>();

		private Map<OWLClass, Integer> ancestorBirdsToMinSteps = null;

		BirdProfile(OWLClass birdCls) {

			this.birdCls = birdCls;

			for (OWLClassAxiom ax : ontology.getAxiomsFor(birdCls)) {

				checkExtractCharacter(ax);
			}
		}

		void checkCreatePair(BirdProfile other) {

			Set<OWLQuantifiedObjectRestriction> sharedChars
				= new HashSet<OWLQuantifiedObjectRestriction>(characters);

			sharedChars.retainAll(other.characters);

			if (sharedChars.size() > 1) {

				birdPairs.add(new BirdPair(sharedChars, this, other));
			}
		}

		Set<OWLClass> getAncestorBirds() {

			ensureAncestorBirds();

			return new HashSet<OWLClass>(ancestorBirdsToMinSteps.keySet());
		}

		int getMinStepsToAncestorBird(OWLClass ancestor) {

			return ancestorBirdsToMinSteps.get(ancestor);
		}

		String getBirdName() {

			return birdCls.getIRI().getShortForm();
		}

		private void ensureAncestorBirds() {

			if (ancestorBirdsToMinSteps == null) {

				ancestorBirdsToMinSteps = new HashMap<OWLClass, Integer>();

				findAncestorBirds(birdCls, 1);
			}
		}

		private void findAncestorBirds(OWLClass current, int steps) {

			for (OWLClass sup : ontology.getDirectSupers(current)) {

				if (checkAncestorBird(sup, steps) && !sup.equals(birdRootCls)) {

					findAncestorBirds(sup, steps + 1);
				}
			}
		}

		private boolean checkAncestorBird(OWLClass ancestor, int steps) {

			Integer minSteps = ancestorBirdsToMinSteps.get(ancestor);

			if (minSteps != null) {

				if (steps < minSteps) {

					ancestorBirdsToMinSteps.put(ancestor, steps);
				}

				return false;
			}

			ancestorBirdsToMinSteps.put(ancestor, steps);

			return true;
		}

		private void checkExtractCharacter(OWLClassAxiom ax) {

			if (ax instanceof OWLSubClassOfAxiom) {

				OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom)ax;

				if (subAx.getSubClass().equals(birdCls)) {

					OWLClassExpression sup = subAx.getSuperClass();

					if (sup instanceof OWLQuantifiedObjectRestriction) {

						characters.add((OWLQuantifiedObjectRestriction)sup);
					}
				}
			}
		}
	}

	private class BirdGroup {

		private Set<OWLQuantifiedObjectRestriction> sharedCharacters;
		private Set<BirdProfile> profiles = new HashSet<BirdProfile>();

		BirdGroup(
			Set<OWLQuantifiedObjectRestriction> sharedCharacters,
			BirdProfile profile1,
			BirdProfile profile2) {

			this.sharedCharacters = sharedCharacters;

			profiles.add(profile1);
			profiles.add(profile2);
		}

		BirdGroup checkCombineWith(BirdGroup other) {

			if (other.sharedCharacters.equals(sharedCharacters)) {

				return new BirdGroup(this, other);
			}

			return null;
		}

		void addDefinitionClass() {

			IRI iri = createDefinitionClassIRI();

			ontology.addEquivalence(ontology.addClass(iri), createDefinition());
		}

		float calcLocalSimilarityScore() {

			return round((float)sharedCharacters.size() / getSampleProfile().characters.size());
		}

		private BirdGroup(BirdGroup group1, BirdGroup group2) {

			sharedCharacters = group1.sharedCharacters;

			profiles.addAll(group1.profiles);
			profiles.addAll(group2.profiles);
		}

		private OWLClassExpression createDefinition() {

			Set<OWLClassExpression> conjuncts = new HashSet<OWLClassExpression>();

			conjuncts.add(birdRootCls);
			conjuncts.addAll(sharedCharacters);

			return ontology.getIntersection(conjuncts);
		}

		private IRI createDefinitionClassIRI() {

			String name = "";

			for (OWLQuantifiedObjectRestriction ch : sharedCharacters) {

				if (name.length() != 0) {

					name += BIRD_GROUP_DEFN_NAME_SEPARATOR;
				}

				name += ontology.getName((OWLProperty)ch.getProperty());
				name += "(" + ontology.getName((OWLClass)ch.getFiller()) + ")";
			}

			return toIRI(name);
		}

		private BirdProfile getSampleProfile() {

			return profiles.iterator().next();
		}
	}

	private class BirdPair extends BirdGroup {

		private BirdProfile profile1;
		private BirdProfile profile2;

		private class PairAnalyser {

			private Set<OWLClass> allAncestorBirds = new HashSet<OWLClass>();
			private Set<OWLClass> commonAncestorBirds = new HashSet<OWLClass>();

			PairAnalyser() {

				Set<OWLClass> ancestors1 = profile1.getAncestorBirds();
				Set<OWLClass> ancestors2 = profile2.getAncestorBirds();

				allAncestorBirds.addAll(ancestors1);
				allAncestorBirds.addAll(ancestors2);

				commonAncestorBirds.addAll(ancestors1);
				commonAncestorBirds.retainAll(ancestors2);
			}

			float calcStructuralSimilarityScore() {

				return round((float)commonAncestorBirds.size() / allAncestorBirds.size());
			}

			int findShortestPathLength() {

				int shortest = Integer.MAX_VALUE;

				for (OWLClass anc : getNearestCommonAncestorBirds()) {

					int current = findInterPairPathLength(anc);

					if (current < shortest) {

						shortest = current;
					}
				}

				return shortest;
			}

			private Set<OWLClass> getNearestCommonAncestorBirds() {

				Set<OWLClass> nearest = new HashSet<OWLClass>(commonAncestorBirds);

				for (OWLClass commonAnc : commonAncestorBirds) {

					nearest.removeAll(ontology.getDirectSupers(commonAnc));
				}

				return nearest;
			}

			private int findInterPairPathLength(OWLClass anc) {

				return profile1.getMinStepsToAncestorBird(anc)
						+ profile2.getMinStepsToAncestorBird(anc);
			}
		}

		BirdPair(
			Set<OWLQuantifiedObjectRestriction> sharedCharacters,
			BirdProfile profile1,
			BirdProfile profile2) {

			super(sharedCharacters, profile1, profile2);

			this.profile1 = profile1;
			this.profile2 = profile2;
		}

		void analyse(AnalysisDoc doc) {

			PairAnalyser analyser = new PairAnalyser();

			doc.addPairAnalysis(
				profile1.getBirdName(),
				profile2.getBirdName(),
				calcLocalSimilarityScore(),
				analyser.calcStructuralSimilarityScore(),
				analyser.findShortestPathLength());
		}
	}

	private BirdyThing(String inFile) {

		ontology = new Ontology(owlFile(inFile));
		birdRootCls = ontology.getClass(BIRD_ROOT_CLS);

		extendOntology();

		ontology.save(owlFile(inFile + OUT_FILE_SUFFIX));
	}

	private void extendOntology() {

		createBirdProfiles();
		createBirdPairs();
		createBirdGroups();

		addBirdGroupDefinitionClasses();

		ontology.reclassify();

		createAnalysisDoc();
	}

	private void createBirdProfiles() {

		for (OWLClass birdCls : ontology.getDirectSubs(birdRootCls)) {

			birdProfiles.add(new BirdProfile(birdCls));
		}
	}

	private void createBirdPairs() {

		for (int i = 0 ; i < birdProfiles.size() - 1 ; i++) {

			BirdProfile profile1 = birdProfiles.get(i);

			for (int j = i + 1 ; j < birdProfiles.size() ; j++) {

				profile1.checkCreatePair(birdProfiles.get(j));
			}
		}
	}

	private void createBirdGroups() {

		for (BirdPair pair : birdPairs) {

			absorbIntoBirdGroups(pair);
		}
	}

	private void absorbIntoBirdGroups(BirdGroup newGroup) {

		BirdGroup oldGroup = null;

		for (BirdGroup currentGroup : birdGroups) {

			BirdGroup comboGroup = currentGroup.checkCombineWith(newGroup);

			if (comboGroup != null) {

				newGroup = comboGroup;
				oldGroup = currentGroup;

				break;
			}
		}

		if (oldGroup != null) {

			birdGroups.remove(oldGroup);
		}

		birdGroups.add(newGroup);
	}

	private void addBirdGroupDefinitionClasses() {

		for (BirdGroup group : birdGroups) {

			group.addDefinitionClass();
		}
	}

	private void createAnalysisDoc() {

		AnalysisDoc doc = new AnalysisDoc();

		for (BirdPair pair : birdPairs) {

			pair.analyse(doc);
		}

		doc.writeToFile();
	}

	private float round(float value) {

		return (float)((int)(value * FLOAT_PRECISION)) / FLOAT_PRECISION;
	}
}
