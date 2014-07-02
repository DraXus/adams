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
 * MyTransformer.java
 * Copyright (C) 2014 Manuel Martin Salvador <msalvador at bournemouth.ac.uk>
 */

package adams.flow.template;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import weka.core.Instances;
import weka.filters.Filter;
import adams.core.ClassLister;
import adams.core.base.BaseRegExp;
import adams.core.base.BaseString;
import adams.core.io.PlaceholderFile;
import adams.data.conversion.SpreadSheetToWekaInstances;
import adams.data.io.input.CsvSpreadSheetReader;
import adams.data.spreadsheet.SpreadSheet;
import adams.env.Environment;
import adams.flow.control.SubProcess;
import adams.flow.core.AbstractActor;
import adams.flow.source.StringConstants;
import adams.flow.transformer.WekaFilter;

public class MyTransformer
  extends AbstractActorTemplate {

  /** for serialization. */
  private static final long serialVersionUID = -4844596229285379292L;

  /** the file to load. */
  protected PlaceholderFile m_TrainingFile;

  /** options for the csv reader */
  protected CsvSpreadSheetReader m_CsvReader;

  /**
   * Returns a string describing the object.
   * 
   * @return a description suitable for displaying in the gui
   */
  @Override
  public String globalInfo() {
    return "MyTransformer used for AutomaticPreprocessing.";
  }

  /**
   * Adds options to the internal list of options.
   */
  public void defineOptions() {
    super.defineOptions();

    m_OptionManager.add("trainingFile", "trainingFile",
	new PlaceholderFile("."));
    m_OptionManager.add("csvReader", "csvReader", new CsvSpreadSheetReader());
  }

  /**
   * Sets the training file to load.
   * 
   * @param value
   *          the file
   */
  public void setTrainingFile(PlaceholderFile value) {
    m_TrainingFile = value;
    reset();
  }

  /**
   * Returns the training file currently to load.
   * 
   * @return the file
   */
  public PlaceholderFile getTrainingFile() {
    return m_TrainingFile;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the gui
   */
  public String trainingFileTipText() {
    return "The training file to load.";
  }

  /**
   * Sets the csv reader.
   * 
   * @param value
   *          the csv reader
   */
  public void setCsvReader(CsvSpreadSheetReader value) {
    m_CsvReader = value;
    reset();
  }

  /**
   * Returns the csv reader.
   * 
   * @return the csv reader
   */
  public CsvSpreadSheetReader getCsvReader() {
    return m_CsvReader;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the gui
   */
  public String csvReaderTipText() {
    return "Options of the CSV reader.";
  }

  /**
   * Hook before generating the actor.
   * <p/>
   * Checks whether the template file exists.
   */
  protected void preGenerate() {
    String variable;

    super.preGenerate();

    variable = getOptionManager().getVariableForProperty("trainingFile");
    if (variable == null) {
      if (!m_TrainingFile.isFile())
	throw new IllegalStateException("'" + m_TrainingFile
	    + "' is not a file!");
    }
  }

  /**
   * Generates the actor.
   * 
   * @return the generated actor
   */
  @Override
  protected AbstractActor doGenerate() {
    System.out.println("Generating filter flow automatically...");
    // 1. Read file
    // TODO Extend to other types of files
    SpreadSheet sheet = m_CsvReader.read(m_TrainingFile);

    // Converting to Weka format to use Weka methods
    SpreadSheetToWekaInstances m_Conversion = new SpreadSheetToWekaInstances();
    m_Conversion.setInput(sheet);
    try {
      String out = m_Conversion.convert();
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    Instances m_Data = (Instances) m_Conversion.getOutput();
    m_Conversion.cleanUp();
    System.out.println("Weka conversion complete");

    // 2. Extract data characteristics
    int numRows = sheet.getRowCount();
    int numColumns = sheet.getColumnCount();
    int[] numMissingValuesByRow = getMissingValuesByRow(m_Data);
    int[] numMissingValuesByColumn = getMissingValuesByColumn(m_Data);

    StringConstants characteristics = new StringConstants();
    characteristics.setStrings(new BaseString[] {
	new BaseString("rows = " + numRows),
	new BaseString("columns = " + numColumns),
	new BaseString("missingValuesByRow = "
	    + Arrays.toString(numMissingValuesByRow)),
	new BaseString("missingValuesByColumn = "
	    + Arrays.toString(numMissingValuesByColumn))});
    // Console report = new Console();

    // listClasses();
    // listFilters();

    List<Class<?>> classes = null;
    try {
      classes = findWekaClasses();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    // 3. Generate workflow based on data characteristics
    // TODO currently it generates a random sequence of filters
    int numOfFilters = 2;
    WekaFilter[] filterList = getRandomFilters(classes, numOfFilters);
    SubProcess seq = new SubProcess();

    for (int i = 0; i < numOfFilters; i++) {
      seq.add(i, filterList[i]);
    }

    System.out.println("Flow generated succesfully");

    return seq;
  }

  /**
   * Generates a random list of Weka filters
   * 
   * @param classes
   *          List of all available weka filters
   * @param numOfFilters
   *          Size of the list to generate
   * @return random list of Weka filters
   */
  private WekaFilter[] getRandomFilters(List<Class<?>> classes, int numOfFilters) {
    WekaFilter[] filterList = new WekaFilter[numOfFilters];

    for (int i = 0; i < numOfFilters; i++) {
      int randomNumber = randInt(0, classes.size());
      try {
	Filter randomFilter = (Filter) classes.get(randomNumber).newInstance();
	System.out.println(randomFilter.toString());
	filterList[i] = new WekaFilter();
	filterList[i].setFilter(randomFilter);
      }
      catch (InstantiationException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
      }
      catch (IllegalAccessException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
      }
    }

    return filterList;
  }

  /**
   * Counts the number of missing values in each row
   * 
   * @return array with the number of missing values in each row
   */
  private int[] getMissingValuesByRow(Instances data) {
    int numRows = data.size();
    int numColumns = data.numAttributes();

    int[] missingValues = new int[numRows];
    for (int i = 0; i < numRows; i++)
      for (int j = 0; j < numColumns; j++)
	if (data.get(i).isMissing(j))
	  missingValues[i]++;

    return missingValues;
  }

  /**
   * Counts the number of missing values in each column
   * 
   * @return array with the number of missing values in each column
   */
  private int[] getMissingValuesByColumn(Instances data) {
    int numRows = data.size();
    int numColumns = data.numAttributes();

    int[] missingValues = new int[numColumns];
    for (int i = 0; i < numRows; i++)
      for (int j = 0; j < numColumns; j++)
	if (data.get(i).isMissing(j))
	  missingValues[j]++;

    return missingValues;
  }

  private void listClasses() {
    try {
      ClassLister m_ClassLister = ClassLister.getSingleton();

      // environment
      String env = null;
      if (env == null)
	env = Environment.class.getName();
      Class cls = Class.forName(env);
      Environment.setEnvironmentClass(cls);

      // match
      // String match = ".*\\.transformer\\..*";
      String match = "adams\\.flow\\.transformer\\.Weka.*";
      if (match == null)
	match = BaseRegExp.MATCH_ALL;
      BaseRegExp regexp = new BaseRegExp(match);

      Class[] implementedFilters = weka.filters.Filter.class
	  .getDeclaredClasses();

      // allow empty class hierarchies?
      boolean allowEmpty = false;

      // superclass
      String[] superclasses;
      String sclass = "adams.flow.core.AbstractActor";
      // String sclass = null;
      if (sclass == null)
	superclasses = m_ClassLister.getSuperclasses();
      else
	superclasses = new String[] {sclass};

      // list them
      for (String superclass: superclasses) {
	cls = Class.forName(superclass);
	String[] classnames = m_ClassLister.getClassnames(cls);
	if ((classnames.length > 0) || allowEmpty) {
	  System.out.println("--> " + superclass);
	  for (String classname: classnames) {
	    if (regexp.isMatch(classname))
	      System.out.println(classname);
	  }
	  System.out.println();
	}
      }
      System.out.println("ClassLister ends");
    }
    catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void listFilters() {
    List<Class<?>> classes;
    try {
      classes = findWekaClasses();
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
    for (Class<?> cl: classes) {
      try {
	System.out.println(cl.getName());
      }
      catch (Throwable t) { // ignore problematic methods and continue
	t.printStackTrace();
	break;
      }
    }
  }

  /**
   * Find the available Weka filters in the jar file
   * 
   * @return list wit the available Weka filters
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private List<Class<?>> findWekaClasses() throws IOException,
      ClassNotFoundException {
    String wekaJarPath = weka.filters.Filter.class.getProtectionDomain()
	.getCodeSource().getLocation().toString();
    wekaJarPath = URLDecoder.decode(wekaJarPath, "UTF-8").replace("file:", "");
    JarFile wekaJar = new JarFile(wekaJarPath);
    Enumeration<JarEntry> contents = wekaJar.entries();
    List<Class<?>> wekaClasses = new ArrayList<Class<?>>(50);
    int badModifiers = Modifier.ABSTRACT | Modifier.INTERFACE
	| Modifier.PRIVATE | Modifier.PROTECTED;
    while (contents.hasMoreElements()) {
      JarEntry entry = contents.nextElement();
      String name = entry.getName();
      if (validClassFile(name)) {
	String className = name.substring(0, name.length() - 6).replaceAll("/",
	    ".");
	Class<?> wekaClass = Class.forName(className);
	if (((wekaClass.getModifiers() & badModifiers) == 0)
	    && weka.filters.Filter.class.isAssignableFrom(wekaClass)
	    && hasDefaultConstructor(wekaClass))
	  wekaClasses.add(wekaClass);
      }
    }
    wekaJar.close();
    return wekaClasses;
  }

  private boolean hasDefaultConstructor(Class<?> wekaClass) {
    for (Constructor<?> c: wekaClass.getConstructors())
      if (c.getParameterTypes().length == 0)
	return true;
    return false;
  }

  private boolean validClassFile(String name) {
    return name.startsWith("weka/filters/") && name.endsWith(".class")
	&& !name.contains("$");
  }

  /**
   * Returns a pseudo-random number between min and max, inclusive. The
   * difference between min and max can be at most
   * <code>Integer.MAX_VALUE - 1</code>.
   * 
   * @param min
   *          Minimum value
   * @param max
   *          Maximum value. Must be greater than min.
   * @return Integer between min and max, inclusive.
   * @see java.util.Random#nextInt(int)
   */
  public static int randInt(int min, int max) {

    // Usually this can be a field rather than a method variable
    Random rand = new Random();

    // nextInt is normally exclusive of the top value,
    // so add 1 to make it inclusive
    int randomNum = rand.nextInt((max - min) + 1) + min;

    return randomNum;
  }

}
