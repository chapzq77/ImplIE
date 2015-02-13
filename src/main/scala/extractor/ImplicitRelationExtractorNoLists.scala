package extractor

import java.util

import com.typesafe.config.{Config, ConfigFactory}
import edu.knowitall.repr.sentence
import edu.knowitall.repr.sentence._
import edu.knowitall.taggers.TaggerCollection
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type
import edu.stanford.nlp.ling.{Sentence, _}
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees._
import utils.{ExtractionUtils, SerializationUtils}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Basic implicit relation extractor.
 *
 * Identifies relation terms from the tagger passed into the constructor.
 * Then expands on specific dependency relations from the Stanford Parser to
 * extract the entity that the relation term modifies.
 *
 * The rules for the extraction are in resources/extractor.conf.
 */
class ImplicitRelationExtractorNoLists(
    tagger: TaggerCollection[sentence.Sentence with Chunked with Lemmatized],
    serializedTokenCacheFile: String = null,
    serializedParseCacheFile: String = null) extends ImplicitRelationExtractor(
    tagger, serializedTokenCacheFile=null, serializedParseCacheFile=null) {

  /**
   * Extracts implicit relations from a string.
   * @param line String, line of text to extract.
   * @return List[ImplicitRelation], list of relations extracted from the string.
   */
  override def extractRelations(line: String): List[ImplicitRelation] = {
    
    val implicitRelations = super.extractRelations(line)
    
    //Filter the implicitRelations, exclude ones which have lists in the entity

    filterNoLists(implicitRelations)
    
  }

  // -----------------------------------------------------------------------
  // Filter out implicitRelations which have a list in the entity 
  //
  // ToDo: define this function, as-is nothing is filtered out
  // -----------------------------------------------------------------------
  def filterNoLists(implicitRelations: List[ImplicitRelation]): List[ImplicitRelation] = {
    
    implicitRelations
    
  }
  
  
}