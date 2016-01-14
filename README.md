# `WordGenerator`

This project is based on https://github.com/stratosphere/peel-wordcount.

## Build

In order to build the `WordGenerator` executable change to the root directory
of the project and run `Maven` using the following command:

    % mvn clean package

This created a fat-jar amond others in the `target` directory.

    target
    ├── ...
    ├── wordgeneratort-1.0-SNAPSHOT-jar-with-dependencies.jar
    └── wordgeneratort-1.0-SNAPSHOT.jar

## Usage

After building the `WordGenerator` executable jar, we can now run it to
generate random words. To get a help message just run `WordGenerator` without
parameters like so

    % java -jar target/wordgeneratort-1.0-SNAPSHOT-jar-with-dependencies.jar
    Usage: <jar> numberOfWords sizeOfDictionary minimumWordLength maximumWordLength


The following command generates 100 words randomly sampled from a 10000 word
dictionary. The last two parameters constraint the length of every word to be
between 2 and 16 (inclusive).

    % java -jar target/wordgeneratort-1.0-SNAPSHOT-jar-with-dependencies.jar 100 10000 2 16
