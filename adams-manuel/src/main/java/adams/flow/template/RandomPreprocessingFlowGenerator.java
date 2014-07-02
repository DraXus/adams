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
 * RandomPreprocessingFlowGenerator.java
 * Copyright (C) 2014 Manuel Martin Salvador <msalvador at bournemouth.ac.uk>
 */

package adams.flow.template;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.filters.Filter;
import adams.core.VariableName;
import adams.core.base.BaseText;
import adams.flow.control.SubProcess;
import adams.flow.core.AbstractActor;
import adams.flow.transformer.SetVariable;
import adams.flow.transformer.WekaFilter;

public class RandomPreprocessingFlowGenerator
  extends AbstractActorTemplate {

  /** for serialization. */
  private static final long serialVersionUID = -9068093590151626425L;

  /** number of consecutive filters **/
  protected int numOfFilters;

  /** maximum number of occurrences for any operator **/
  protected int maxNumOccurrences;

  private HashMap<String, Integer> counter = new HashMap<String, Integer>();

  /**
   * Returns a string describing the object.
   * 
   * @return a description suitable for displaying in the gui
   */
  @Override
  public String globalInfo() {
    return "Generate a random batch preprocesing flow";
  }

  /**
   * Get the number of filters
   * 
   * @return numOfFilters
   */
  public int getNumOfFilters() {
    return numOfFilters;
  }

  /**
   * Set the number of filters
   * 
   * @param numOfFilters
   */
  public void setNumOfFilters(int numOfFilters) {
    this.numOfFilters = numOfFilters;
    reset();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the gui
   */
  public String numOfFiltersTipText() {
    return "The number of filters to generate (must be positive).";
  }

  /**
   * Get the maximum number of occurrences for any operator
   * 
   * @return maxNumOccurrences
   */
  public int getMaxNumOccurrences() {
    return maxNumOccurrences;
  }

  /**
   * Set the maximum number of occurrences for any operator
   * 
   * @param maxNumOccurrences
   */
  public void setMaxNumOccurrences(int maxNumOccurrences) {
    this.maxNumOccurrences = maxNumOccurrences;
    reset();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the gui
   */
  public String maxNumOccurrencesTipText() {
    return "The maximum number of occurrences for any operator (must be positive).";
  }

  /**
   * Adds options to the internal list of options.
   */
  public void defineOptions() {
    super.defineOptions();

    m_OptionManager.add("num-filters", "numOfFilters", 0);
    m_OptionManager.add("max-num-occurrences", "maxNumOccurrences", 0);

  }

  /**
   * Hook before generating the actor.
   */
  protected void preGenerate() {
    super.preGenerate();

  }

  /**
   * Generates the actor.
   * 
   * @return the generated actor
   */
  @Override
  protected AbstractActor doGenerate() {
    System.out.println("Generating filter flow automatically (filters = "
	+ numOfFilters + ")...");

    if (numOfFilters <= 0) {
      throw new IllegalStateException("The number of filters must be positive");
    }

    if (maxNumOccurrences < 0) {
      throw new IllegalStateException(
	  "The maximum number of occurrences must be positive");
    }
    else if(maxNumOccurrences == 0){ //if default value
      maxNumOccurrences = numOfFilters;
    }

    List<Class<?>> classes = null;
    try {
      classes = findWekaClasses();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    //listFilters(false);
    listFilters();

    // Generates a random sequence of filters
    WekaFilter[] filterList = getRandomFilters(classes, numOfFilters);
    SubProcess seq = new SubProcess();

    String filterNamesString = "";

    // Create sequence of filters
    for (int i = 0; i < numOfFilters; i++) {
      seq.add(i, filterList[i]);
      filterNamesString += filterList[i].getFilter().toString() + ",";
    }
    // Remove last comma from the list
    if (filterNamesString.endsWith(",")) {
      filterNamesString = filterNamesString.substring(0,
	  filterNamesString.length() - 1);
    }

    // Create variable with the name of filters for log purposes
    SetVariable filterNames = new SetVariable();
    filterNames.setVariableName(new VariableName("filters"));
    BaseText temp = new BaseText();
    temp.setValue(filterNamesString);
//    filterNames.setVariableValue(temp);

    seq.add(0, filterNames);

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

      try {
	Filter randomFilter;
	Integer filterOccurrence;
	String filterName;
	
	int[] discard = new int[classes.size()];

	do {
	  int randomNumber = randInt(0, classes.size() - 1);
	  randomFilter = (Filter) classes.get(randomNumber).newInstance();
	  filterName = randomFilter.toString();

	  // Increment counter of occurrences for the current filter
	  filterOccurrence = counter.get(filterName);
	  if (filterOccurrence == null) {
	    filterOccurrence = 0;
	  }
	  
	  // Control if the max number of occurrences have been reached by all the filters 
	  discard[randomNumber] = 1;
	  if(sum(discard)==discard.length){
	    throw new Exception("All filters have been already used and it is not possible to add more");
	  }
	}
	while (filterOccurrence > maxNumOccurrences);

	counter.put(filterName, ++filterOccurrence);

	System.out.println(filterName);
	filterList[i] = new WekaFilter();
	filterList[i].setFilter(randomFilter);
	// Initialize only with the initial batch
	filterList[i].setInitializeOnce(true); 
	// filterList[i].setStopFlowOnError(false);
      }
      catch (InstantiationException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
      }
      catch (IllegalAccessException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
      }
      catch (Exception e){
	e.printStackTrace();
      }
    }

    return filterList;
  }

  private void listFilters() {
    listFilters(true);
  }

  private void listFilters(boolean listOptions) {
    List<Class<?>> classes;
    try {
      classes = findWekaClasses();
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
    int countParameters = 0;
    for (Class<?> cl: classes) {
      try {
	System.out.println(cl.getName());
	
	if (listOptions)
	  countParameters += listParameters(cl);
      }
      catch (Throwable t) { // ignore problematic methods and continue
	t.printStackTrace();
	break;
      }
    }
    System.out.println("Number of filters: " + classes.size());
    if (listOptions)
      System.out.println("Number of parameters: " + countParameters);
  }

  private int listParameters(Class<?> filter) {
    if (OptionHandler.class.isAssignableFrom(filter)) {

      try {
	int counter = 0;
	Enumeration<Option> options = ((OptionHandler) filter.newInstance())
	    .listOptions();
	for (Option o = options.nextElement(); options.hasMoreElements(); o = options
	    .nextElement()) {
	  System.out.println(o.name() + " (" + o.numArguments() + ") "
	      + o.description());
	  counter++;
	}
	return counter;
      }
      catch (InstantiationException e) {
	e.printStackTrace();
      }
      catch (IllegalAccessException e) {
	e.printStackTrace();
      }

    }
    return 0;

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
  
  private int sum(int[] array){
    int total = 0;
    for(int i : array){
      total += i;
    }
    return total;
  }

}
