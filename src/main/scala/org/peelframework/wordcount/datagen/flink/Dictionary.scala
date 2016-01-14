package org.peelframework.wordcount.datagen.flink

import org.peelframework.wordcount.datagen.util.RanHash

class Dictionary (seed : Long, size : Int, minLength : Int = 2, maxLength : Int = 16) extends Iterable[String] {

  private val NUM_CHARACTERS = 26

  private val random : RanHash = new RanHash(seed)

  override def iterator: Iterator[String] = words.iterator

  def word (index : Int) : String = {
    require(0 <= index && index < size)
    // skip to the correct position within the random sequence
    // assume that every word needs 1 random value for the length and max_length random integers for the actual word
    random.skipTo(index * (maxLength + 1))
    val wordLength = random.nextInt(maxLength - minLength + 1) + minLength
    val strBld = new StringBuilder(wordLength)
    for (i <- 0 until wordLength) {
        val c = ('a'.toInt + random.nextInt(NUM_CHARACTERS)).toChar
        strBld.append(c)
      }
    strBld.mkString
  }

  def words () : Array[String] = (0 until size).map(i => word(i)).toArray
}
