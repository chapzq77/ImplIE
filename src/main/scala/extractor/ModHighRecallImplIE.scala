package extractor

import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence.{Lemmatized, Chunked}
import edu.knowitall.taggers.TaggerCollection

/**
 * Extractor with a subset of the normal filters and generalized expansion rules.
 * For most recall, the tagger should use case insensitive taggers with only
 * the dirtiest tags ignored.
 */
class ModHighRecallImplIE(
    tagger: TaggerCollection[sentence.Sentence with Chunked with Lemmatized],
    serializedTokenCacheFile: String = null,
    serializedParseCacheFile: String = null)
  extends ImplicitRelationExtractorNoLists(tagger, serializedTokenCacheFile,
    serializedParseCacheFile, "high-recall-extractor.conf")
  with BasicFilterFunctions {

  override def extractRelations(line: String): List[ImplicitRelation] = {
    val relations = super.extractRelations(line)
    filter(line, relations)
  }

  // Avoid the head filter.
  override def headUnfilteredExtractions(line: String): List[ImplicitRelation] = {
    val relations = super.unfilteredExtractions(line)
    filter(line, relations)
  }

  private def filter(line: String, relations: List[ImplicitRelation]) = {
    var filtered = filterDaysOfWeek(line, relations)
    filtered = filterStrangeDateFormats(line, filtered)
    filtered = filterVsEntities(line, filtered)
    filtered = filterTagIsEntity(line, filtered)
    filtered
  }
}