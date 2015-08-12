package org.peelframework.wordcount

import com.samskivert.mustache.Mustache
import com.typesafe.config.ConfigFactory
import org.peelframework.core.beans.data.{CopiedDataSet, DataSet, ExperimentOutput, GeneratedDataSet}
import org.peelframework.core.beans.experiment.ExperimentSequence.SimpleParameters
import org.peelframework.core.beans.experiment.{ExperimentSequence, ExperimentSuite}
import org.peelframework.core.beans.system.Lifespan
import org.peelframework.flink.beans.job.FlinkJob
import org.peelframework.flink.beans.system.Flink
import org.peelframework.hadoop.beans.system.HDFS2
import org.peelframework.spark.beans.experiment.SparkExperiment
import org.peelframework.spark.beans.system.Spark
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

/** Experiments definitions for the 'peel-wordcount' bundle. */
@Configuration
class ExperimentsDefinitions extends ApplicationContextAware {

  /* The enclosing application context. */
  var ctx: ApplicationContext = null

  def setApplicationContext(ctx: ApplicationContext): Unit = {
    this.ctx = ctx
  }

  // ---------------------------------------------------
  // Systems
  // ---------------------------------------------------

  @Bean(name = Array("hdfs-2.7.1"))
  def `hdfs-2.7.1`: HDFS2 = new HDFS2(
    version      = "2.7.1",
    configKey    = "hadoop-2",
    lifespan     = Lifespan.SUITE,
    mc           = ctx.getBean(classOf[Mustache.Compiler])
  )

  @Bean(name = Array("flink-0.9.0"))
  def `flink-0.9.0`: Flink = new Flink(
    version      = "0.9.0",
    configKey    = "flink",
    lifespan     = Lifespan.EXPERIMENT,
    dependencies = Set(ctx.getBean("hdfs-2.7.1", classOf[HDFS2])),
    mc           = ctx.getBean(classOf[Mustache.Compiler])
  )

  @Bean(name = Array("spark-1.4.0"))
  def `spark-1.4.0`: Spark = new Spark(
    version      = "1.4.0",
    configKey    = "spark",
    lifespan     = Lifespan.EXPERIMENT,
    dependencies = Set(ctx.getBean("hdfs-2.7.1", classOf[HDFS2])),
    mc           = ctx.getBean(classOf[Mustache.Compiler])
  )

  // ---------------------------------------------------
  // Data Generators
  // ---------------------------------------------------

  @Bean(name = Array("datagen.words"))
  def `datagen.words`: FlinkJob = new FlinkJob(
    runner  = ctx.getBean("flink-0.9.0", classOf[Flink]),
    command =
      """
        |-v -c org.peelframework.wordcount.datagen.flink.WordGenerator        \
        |${app.path.datagens}/peel-wordcount-datagens-1.0-SNAPSHOT.jar        \
        |${system.default.config.parallelism.total}                           \
        |${datagen.tuples.per.task}                                           \
        |${datagen.dictionary.dize}                                           \
        |${datagen.data-distribution}                                         \
        |${system.hadoop-2.path.input}/rubbish.txt
      """.stripMargin.trim
  )

  // ---------------------------------------------------
  // Data Sets
  // ---------------------------------------------------

  @Bean(name = Array("dataset.words.static"))
  def `dataset.words.static`: DataSet = new CopiedDataSet(
    src = "${app.path.datasets}/rubbish.txt",
    dst = "${system.hadoop-2.path.input}/rubbish.txt",
    fs  = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("dataset.words.generated"))
  def `dataset.words.generated`: DataSet = new GeneratedDataSet(
    src = ctx.getBean("datagen.words", classOf[FlinkJob]),
    dst = "${system.hadoop-2.path.input}/rubbish.txt",
    fs  = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  @Bean(name = Array("wordcount.output"))
  def `wordcount.output`: ExperimentOutput = new ExperimentOutput(
    path = "${system.hadoop-2.path.output}/wordcount",
    fs  = ctx.getBean("hdfs-2.7.1", classOf[HDFS2])
  )

  // ---------------------------------------------------
  // Experiments
  // ---------------------------------------------------

  @Bean(name = Array("wordcount.default"))
  def `wordcount.default`: ExperimentSuite = {
    val `wordcount.spark.default` = new SparkExperiment(
      name    = "wordcount.spark.default",
      command =
        """
          |--class org.peelframework.wordcount.spark.SparkWC                    \
          |${app.path.apps}/peel-wordcount-spark-jobs-1.0-SNAPSHOT.jar          \
          |${system.hadoop-2.path.input}/rubbish.txt                            \
          |${system.hadoop-2.path.output}/wordcount
        """.stripMargin.trim,
      config  = ConfigFactory.parseString(""),
      runs    = 3,
      runner  = ctx.getBean("spark-1.4.0", classOf[Spark]),
      inputs  = Set(ctx.getBean("dataset.words.static", classOf[DataSet])),
      outputs = Set(ctx.getBean("wordcount.output", classOf[ExperimentOutput]))
    )

    new ExperimentSuite(Seq(
      `wordcount.spark.default`))
  }

  @Bean(name = Array("wordcount.scale-out"))
  def `wordcount.scale-out`: ExperimentSuite = {
    val `wordcount.spark.prototype` = new SparkExperiment(
      name    = "wordcount.spark.__topXXX__",
      command =
        """
          |--class org.peelframework.wordcount.spark.SparkWC                    \
          |${app.path.apps}/peel-wordcount-spark-jobs-1.0-SNAPSHOT.jar          \
          |${system.hadoop-2.path.input}/rubbish.txt                            \
          |${system.hadoop-2.path.output}/wordcount
        """.stripMargin.trim,
      config  = ConfigFactory.parseString(
        """
          |system.default.config.slaves            = ${env.slaves.__topXXX__.hosts}
          |system.default.config.parallelism.total = ${env.slaves.__topXXX__.total.parallelism}
          |datagen.dictionary.dize                 = 10000
          |datagen.tuples.per.task                 = 10000000 # ~ 100 MB
          |datagen.data-distribution               = Uniform
        """.stripMargin.trim),
      runs    = 3,
      runner  = ctx.getBean("spark-1.4.0", classOf[Spark]),
      inputs  = Set(ctx.getBean("dataset.words.generated", classOf[DataSet])),
      outputs = Set(ctx.getBean("wordcount.output", classOf[ExperimentOutput]))
    )

    new ExperimentSuite(
      new ExperimentSequence(
        parameters = new SimpleParameters(
          paramName = "topXXX",
          paramVals = Seq("top005", "top010", "top020")),
        prototypes = Seq(
          `wordcount.spark.prototype`)))
  }
}
