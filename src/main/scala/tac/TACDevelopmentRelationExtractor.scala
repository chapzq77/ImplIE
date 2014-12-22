package tac

import java.io.PrintWriter
import java.nio.file.{Paths, Files}

import com.typesafe.config.ConfigFactory
import extractor.{TaggerLoader, NounToNounRelationExtractor}

import scala.io.Source

/**
 * Created by Gene on 12/21/2014.
 */
object TACDevelopmentRelationExtractor {
  case class InputLine(index: Int, file: String, sentence: String)

  val config = ConfigFactory.load("tac-extractor.conf")
  val resultDir = config.getString("result-dir")
  val sentenceDir = config.getString("sentence-dir")
  val seqFilename = config.getString("sequence-file")
  val seq = getSeqNum(seqFilename)

  def main(args: Array[String]) {
    val relationExtractor =
      new NounToNounRelationExtractor(TaggerLoader.defaultTagger)

    val inputLines = input
    val out = output

    for (inputLine <- inputLines) {
      val extraction = relationExtractor.extractRelations(inputLine.sentence)
      out.println(s"${inputLine.index}\t${inputLine.file}\t$extraction")
    }

    out.close()
  }

  def input = {
    // TODO: put the file name structure in the config so that the change can be
    // made in one place without breaking anything.
    val filename = sentenceDir + seq + "-sentence-file"

    // Check if the file exists.
    if (!Files.exists(Paths.get(filename))) {
      System.out.println(s"Sentence file doesn't exist!  " +
        s"Please run the TACDevelopmentSentenceExtractor first, " +
        s"or check that the sequence number in $seqFilename is correct.\n" +
        s"Exiting...")
      sys.exit(1)
    }

    // TODO: put the delimiter in config
    Source.fromFile(filename).getLines().map(line => {
      val tokens = line.trim.split("\t")
      InputLine(tokens(0).asInstanceOf[Int], tokens(1), tokens(2))
    })
  }

  def output = {
    val filename = resultDir + seq + "-result-file"

    // Check if the file exists.
    if (Files.exists(Paths.get(filename))) {
      System.out.println(s"Sentence file already exists!  " +
        s"Please set the number in $seqFilename to a non conflicting " +
        s"sequence number.\nExiting...")
      sys.exit(1)
    }

    // If not, create new file with the given sequence num.
    new PrintWriter(filename)
  }

  // This method is a bit of a hack.
  // We know that the sentence num is incremented after the sentence extractor,
  // so we take one less than what is recorded there.
  def getSeqNum(sequenceFile: String) = Source.fromFile(sequenceFile)
    .getLines().next().trim.asInstanceOf[Int]
}