package extractor

import com.typesafe.config.{ConfigFactory, Config}
import edu.knowitall.repr.sentence._
import edu.knowitall.taggers.{TaggerRule, ParseRule, TaggerCollection}
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.typer.Type

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Object for loading taggers from configurations.
 * There are pre-specified tagger configurations for most needs as well as
 * a buildTagger function that takes a Config for a tagger.
 */
object TaggerLoader {
  case class TagClass(name: String, taggerType: String, files: List[String], ignoreFiles: List[String])
  case class TaggerResult(tags: List[Type], source: String)

  val basic_test_tagger_config = ConfigFactory.load("taggers/basic-test-tagger.conf")
  val no_tags_ignored_config = ConfigFactory.load("taggers/no-tags-ignored-tagger.conf")
  val corrected_cap_config = ConfigFactory.load("taggers/corrected-cap-tagger.conf")
  val uncorrected_cap_config = ConfigFactory.load("taggers/uncorrected-cap-tagger.conf")
  val high_recall_config = ConfigFactory.load("taggers/high-recall-tagger.conf")
  val extended_keyword_config = ConfigFactory.load("taggers/extended-keyword-tagger.conf")
  val extended_keyword_high_recall_config = ConfigFactory.load("taggers/extended-keyword-high-recall-tagger.conf")

  // Experimental taggers.
  val un_city_extension = ConfigFactory.load("taggers/experimental/UN-extension.conf")
  val un_city_high_recall = ConfigFactory.load("taggers/experimental/UN-high-recall.conf")
  val freebase_extension = ConfigFactory.load("taggers/experimental/freebase-extension.conf")

  val chunker = new OpenNlpChunker()

  val taggerMemo = scala.collection.mutable.Map
    [String, TaggerCollection[Sentence with Chunked with Lemmatized]]()
  val taggerRuleMemo = scala.collection.mutable.Map
    [Int, Seq[edu.knowitall.taggers.Rule[Sentence with Chunked with Lemmatized]]]()
  def memoizedTagger(key: String, tagger_config: Config)() = {
    taggerMemo.get(key) match {
      case None =>
        val result = buildTagger(tagger_config)
        taggerMemo.put(key, result)
        result
      case Some(result) => result
    }
  }

  def defaultTagger = memoizedTagger("default", corrected_cap_config)
  def basicTestTagger = memoizedTagger("basic_test", basic_test_tagger_config)
  def noTagsIgnoredTagger = memoizedTagger("no_tags_ignored", no_tags_ignored_config)
  def correctedCapTagger = memoizedTagger("corrected_cap", corrected_cap_config)
  def uncorrectedCapTagger = memoizedTagger("uncorrected_cap", uncorrected_cap_config)
  def highRecallTagger = memoizedTagger("high_recall", high_recall_config)
  def extendedKeywordTagger = memoizedTagger("extended_keyword", extended_keyword_config)
  def extendedKeyworkHighRecallTagger = memoizedTagger("extended_keyword_high_recall", extended_keyword_high_recall_config)

  // Experimental... 
  def unExperimentalTagger = memoizedTagger("un_experimental", un_city_extension)
  def unExperimentalHighRecallTagger = memoizedTagger("un_experimental_high_recall", un_city_high_recall)
  def freebaseExperimentalTagger = memoizedTagger("freebase_experimental", freebase_extension)

  /**
   * Constructs a tagger from a tagger configuration.
   * @param config Configuration for the tagger.
   * @return TaggerCollection (tagger).
   */
  def buildTagger(config: Config): TaggerCollection[Sentence with Chunked with Lemmatized] = {
    /**
     * Builds string with the definitions of class term relation for tagger.
     * @param classes List of class to term list mappings.
     * @return String definitions of each class.
     */
    def createTaggerDefinition(classes: List[TagClass]): String = {
      def punctuationSeparated(term: String) = {
        val punct = Set[(String, String)](("\\.", "."), (",", ","), ("'", "'"), ("\"", "\""))
        var modified = term
        for (p <- punct) {
          val tokens = modified.split(p._1)
          modified = tokens.mkString(s" ${p._2}")
        }
        modified
      }

      def termsFromFile(file: String, caseInsensitive: Boolean): Set[String] = {
        // Trim and add versions with separated out apostrophies and periods.
        val orig = Source.fromFile(file).getLines().map(term => term.trim)
          .filter(term => term != "").toSet
        val result = orig ++ orig.map(term => punctuationSeparated(term))
        if (caseInsensitive) result.map(term => term.toLowerCase) else result
      }

      val builder = StringBuilder.newBuilder
      for (clas <- classes) {
        builder.append(s"${clas.name} := ${clas.taggerType} {\n")
        val caseInsensitive = clas.taggerType.toLowerCase.contains("caseinsensitive")
        val terms = clas.files.foldLeft(Set[String]())((acc, cur) =>
          acc ++ termsFromFile(cur, caseInsensitive))
        val ignoreTerms = clas.ignoreFiles.foldLeft(Set[String]())((acc, cur) =>
          acc ++ termsFromFile(cur, caseInsensitive))
        (terms -- ignoreTerms).foreach(term => builder.append(term).append("\n"))
        builder.append("}\n")
      }
      builder.mkString
    }

    def getClasses: List[TagClass] = {
      val classes: List[Config] = config.getConfigList("classes").toList
      classes.map(c => TagClass(c.getString("name"), c.getString("tagger-type"),
          c.getStringList("files").toList,
          if (c.hasPath("ignore-files")) {
            c.getStringList("ignore-files").toList
          } else {
            Nil
          }))
    }

    println("Loading tagger...")
    val start = System.nanoTime()
    val taggerPattern = createTaggerDefinition(getClasses)

    // Setup structures for representing data.
    val rules = new ParseRule[Sentence with Chunked with Lemmatized].parse(taggerPattern).get
    taggerRuleMemo.put(config.hashCode(), rules)

    val ret = rules.foldLeft(new TaggerCollection[Sentence with Chunked with Lemmatized]()){ case (ctc, rule) => ctc + rule }
    val end = System.nanoTime()
    val diff = (end - start).toDouble / 1000000000
    println(f"Tagger building complete. [$diff%.3f sec]")
    ret
  }

  private def taggerFunction
    (tagger: TaggerCollection[Sentence with Chunked with Lemmatized])
    (line: String): TaggerResult = {
    def process(text: String): Sentence with Chunked with Lemmatized = {
      new Sentence(text) with Chunker with Lemmatizer {
        val chunker = TaggerLoader.chunker
        val lemmatizer = MorphaStemmer
      }
    }
    TaggerResult(tagger.tag(process(line)).toList, line)
  }
}
