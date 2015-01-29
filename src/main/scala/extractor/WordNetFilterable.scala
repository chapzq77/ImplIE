package extractor

import edu.stanford.nlp.trees.{TypedDependency, Tree, SemanticHeadFinder}

/**
 * Trait for any class that wants to be able to filter extractions by WordNet
 * attributes.
 *
 * Very general, meant to be like a Java interface.  This trait should be used
 * by other traits that implement filterWordNet for a specific filtering.
 * Then use with ImplIE and call to filter normal extraction relations.
 *
 * filterWordNet is the function that will be used by subclasses to filter
 * WordNet.
 */
trait WordNetFilterable {
  def getParse(line: String): (Tree, List[TypedDependency])

  // TODO: Add index to the head.
  // TODO: add WordNetStemmer
  // TODO: move to normal ImplicitRelationExtractor
  def addHeadsToExtractions(extractions: List[ImplicitRelation]) {
    // Get heads of the extractions.
    val headFinder = new SemanticHeadFinder()
    extractions.foreach(rel => {
      val tree = getParse(rel.np.string)._1
      rel.setHead(tree.headTerminal(headFinder).value())
    })
  }

  def filterWordNet(line: String, relations: List[ImplicitRelation]): List[ImplicitRelation]
}