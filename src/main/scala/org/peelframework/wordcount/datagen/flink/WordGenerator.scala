package org.peelframework.wordcount.datagen.flink

import org.peelframework.wordcount.datagen.flink.Distributions.{Zipf, Binomial, DiscreteUniform, DiscreteDistribution}
import org.peelframework.wordcount.datagen.util.RanHash

object WordGenerator {

  val SEED = 0xC00FFEE

  def main(args: Array[String]): Unit = {

    if (args.length != 4) {
      Console.err.println("Usage: <jar> numberOfWords sizeOfDictionary minimumWordLength maximumWordLength")
      System.exit(-1)
    }

    val numberOfWords         = args(0).toInt
    val sizeOfDictionary      = args(1).toInt
    val minLength             = args(2).toInt
    val maxLength             = args(3).toInt

//    implicit val distribution = parseDist(sizeOfDictionary, args(2))
    implicit val distribution = parseDist(sizeOfDictionary, "Uniform")



    // generate dictionary of random words
    implicit val dictionary = new Dictionary(SEED, sizeOfDictionary, minLength, maxLength).words()

    // create a sequence [1 .. N] to create N words
    // map every n <- [1 .. N] to a random word sampled from a word list
    (1L to numberOfWords) map (i => word(i)) map (w => println(w))
  }

  def word(i: Long)(implicit dictionary: Array[String], distribution: DiscreteDistribution) = {
    dictionary(distribution.sample(new RanHash(SEED + i).next()))
  }

  object Patterns {
    val DiscreteUniform = """(Uniform)""".r
    val Binomial = """Binomial\[(1|1\.0|0\.\d+)\]""".r
    val Zipf = """Zipf\[(\d+(?:\.\d+)?)\]""".r
  }

  def parseDist(card: Int, s: String): DiscreteDistribution = s match {
    case Patterns.DiscreteUniform(_) => DiscreteUniform(card)
    case Patterns.Binomial(a) => Binomial(card, a.toDouble)
    case Patterns.Zipf(a) => Zipf(card, a.toDouble)
  }

}
