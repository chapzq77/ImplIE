package extractor

import edu.stanford.nlp.trees.TypedDependency

/**
 * TODO: complete
 * tag = tagged phrase
 * relation = class (nationality, jobTitle, etc)
 * np = noun phrase
 * sentence = original sentence
 * relationTrace = trace of dependency relations hopped to make the extraction
 */
class ImplicitRelation(t: IndexedString, r: String, n: IndexedSubstring,
                         s: String = "", rt: List[TypedDependency] = Nil) {
  var tag = t
  var relation = r
  var np = n
  var sentence = s
  var relationTrace = rt
  var ners: List[NERTag] = Nil

  override def toString: String = s"($tag, $relation, $np)"
  def longString: String = s"($tag, $relation, $np), $sentence"

  def addNER(nerTag: NERTag) {
    ners = nerTag::ners
  }

  def setNERs(nerTags: List[NERTag]) {
    ners = nerTags
  }

  def getNERs = ners
}
