package extractor

import java.net.URL

import com.typesafe.config.ConfigFactory
import edu.knowitall.repr.sentence.{Lemmatized, Chunked, Sentence}
import edu.knowitall.taggers.TaggerCollection
import edu.mit.jwi.Dictionary
import edu.mit.jwi.morph.WordnetStemmer

import scala.collection.JavaConversions._

/**
 * A version of ImplIE that we constrained for a hypothetical constrained usage.
 * The specifications of the constraints are under designDocs/ConstrainedImplIE.*.
 * Built on top of high-recall ImplIE rather than high-precision ImplIE.
 *
 * Roughly the specifications say that the extracted entity may only be a
 * person or an organization, and for some tags only a person.
 * This implementation accomplished this by filtering by WordNet hypernyms
 * and NER tags on the head word of the entity.
 *
 * The WordNet hypernyms capture information about any entity whose head word
 * is a word in a dictionary (i.e. not a proper noun).
 * The NER tags capture information about entites that are names, and thus
 * not included in the WordNet dictionary.
 */
class ConstrainedHighRecallImplIE
  (tagger: TaggerCollection[Sentence with Chunked with Lemmatized],
   serializedTokenCacheFile: String = null,
   serializedParseCacheFile: String = null)
  extends ModHighRecallImplIE(
    tagger, serializedTokenCacheFile, serializedParseCacheFile)
  // filterWordNet function.
  with WordNetHypernymFilter
  // filterNERs function.
  with NERFilterByHeadWord {

  protected val wordnetConfig = ConfigFactory.load("wordnet-filtered-ire.conf")
  protected val wordnetFilterParams =
    getWordnetFilters(wordnetConfig.getConfigList("wordnet-filters").toList)
  protected val wordnetDictionary = new Dictionary(new URL(
    "file", null, wordnetConfig.getString("wordnet-dictionary")))
  val stemmer = new WordnetStemmer(wordnetDictionary)
  val wordSenseLimit = wordnetConfig.getInt("word-sense-limit")

  val constrainedNerConfig = ConfigFactory.load("ner-filtered-ire.conf")
  protected val constrainedExpectedEntities =
    expectedTagEntities(constrainedNerConfig.getConfigList("tag-entities").toList)
  protected val CONSTRAINED_NER_TAGS_TO_IGNORE = constrainedNerConfig.getStringList("ner-tag-ignore").toList

  override def extractRelations(line: String): List[ImplicitRelation] = {
    val unfiltered = super.extractRelations(line)
    // Return everything that satisfies WordNet or NER.
    val wordnetFiltered = filterWordNet(line, unfiltered)
    val nerFiltered = filterNERsByHeadWord(line, unfiltered, CONSTRAINED_NER_TAGS_TO_IGNORE, constrainedExpectedEntities)
    (wordnetFiltered.toSet union nerFiltered.toSet).toList
  }
}
