/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * WekaEvaluationSummaryTest.java
 * Copyright (C) 2010-2014 University of Waikato, Hamilton, New Zealand
 */

package adams.flow.template;

import java.nio.channels.Pipe.SinkChannel;

import junit.framework.Test;
import junit.framework.TestSuite;
import adams.core.io.PlaceholderFile;
import adams.env.Environment;
import adams.flow.AbstractFlowTest;
import adams.flow.control.Flow;
import adams.flow.core.AbstractActor;
import adams.flow.core.CallableActorReference;
import adams.flow.sink.Console;
import adams.flow.sink.DumpFile;
import adams.flow.source.FileSupplier;
import adams.flow.source.WekaClassifierSetup;
import adams.flow.standalone.CallableActors;
import adams.flow.transformer.TemplateTransformer;
import adams.flow.transformer.TemplateTransformerNoCache;
import adams.flow.transformer.WekaClassSelector;
import adams.flow.transformer.WekaCrossValidationEvaluator;
import adams.flow.transformer.WekaEvaluationSummary;
import adams.flow.transformer.WekaFileReader;
import adams.flow.transformer.WekaFileReader.OutputType;
import adams.test.AbstractTestHelper;
import adams.test.TestHelper;
import adams.test.TmpFile;

/**
 * Tests MyTransformer actor template.
 * 
 * @author msalvador (msalvador at bournemouth.ac.uk)
 * @version $Revision: 8665 $
 */
public class RandomPreprocessingFlowGeneratorTest
  extends AbstractFlowTest {

  final private String inputFile = "RandomRBF-1k.arff";

  final private String trainingFile = "RandomRBF-1k-copy.csv";

  final private String outputFile = "dumpfile.txt";

  /**
   * Initializes the test.
   * 
   * @param name
   *          the name of the test
   */
  public RandomPreprocessingFlowGeneratorTest(String name) {
    super(name);
  }

  /**
   * Called by JUnit before each test method.
   * 
   * @throws Exception
   *           if an error occurs
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    m_TestHelper.copyResourceToTmp(inputFile);
    m_TestHelper.copyResourceToTmp(trainingFile);
    m_TestHelper.deleteFileFromTmp(outputFile);
  }

  /**
   * Called by JUnit after each test method.
   * 
   * @throws Exception
   *           if tear-down fails
   */
  @Override
  protected void tearDown() throws Exception {
    m_TestHelper.deleteFileFromTmp(inputFile);
    m_TestHelper.deleteFileFromTmp(trainingFile);
    m_TestHelper.deleteFileFromTmp(outputFile);

    super.tearDown();
  }

  /**
   * Used to create an instance of a specific actor.
   * 
   * @return a suitably configured <code>AbstractActor</code> value
   */
  public AbstractActor getActor() {
    System.out.println("getting actor");
    WekaClassifierSetup cls = new WekaClassifierSetup();
    cls.setName("cls");
    cls.setClassifier(new weka.classifiers.trees.J48());

    CallableActors ga = new CallableActors();
    ga.setActors(new AbstractActor[] {cls});

    FileSupplier sfs = new FileSupplier();
    sfs.setFiles(new adams.core.io.PlaceholderFile[] {new TmpFile(inputFile)});

    WekaFileReader fr = new WekaFileReader();
    fr.setOutputType(OutputType.DATASET);

    WekaClassSelector cs = new WekaClassSelector();
    
    RandomPreprocessingFlowGenerator tr = new RandomPreprocessingFlowGenerator();
    tr.setNumOfFilters(1);
    tr.setMaxNumOccurrences(1);

    TemplateTransformerNoCache template = new TemplateTransformerNoCache();
    template.setTemplate(tr);

    WekaCrossValidationEvaluator cv = new WekaCrossValidationEvaluator();
    cv.setClassifier(new CallableActorReference("cls"));

    WekaEvaluationSummary eval = new WekaEvaluationSummary();
    DumpFile df = new DumpFile();
    df.setOutputFile(new TmpFile(outputFile));
    
    //Console df = new Console();

    Flow flow = new Flow();
    flow.setActors(new AbstractActor[] {ga, sfs, fr, cs, template, cv, eval, df});

    return flow;
  }

  /**
   * Returns a test suite.
   * 
   * @return the test suite
   */
  public static Test suite() {
    return new TestSuite(RandomPreprocessingFlowGeneratorTest.class);
  }
  
  @Override
  protected synchronized void connect() {
    // TODO Auto-generated method stub
    //super.connect();
  }
  
  
  @Override
  protected synchronized void reconnect(String file) {
    // TODO Auto-generated method stub
    //super.reconnect(file);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args
   *          ignored
   */
  public static void main(String[] args) {
    Environment.setEnvironmentClass(Environment.class);
    runTest(suite());
  }
}
