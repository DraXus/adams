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
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import weka.filters.Filter;
import adams.flow.control.SubProcess;
import adams.flow.core.AbstractActor;
import adams.flow.transformer.WekaFilter;

public class RandomPreprocessingFlowGenerator extends AbstractActorTemplate {

	/** for serialization. */
	private static final long serialVersionUID = -9068093590151626425L;

	/** number of consecutive filters **/
	protected int numOfFilters;

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
	 * Adds options to the internal list of options.
	 */
	public void defineOptions() {
		super.defineOptions();

		m_OptionManager.add("num-filters", "numOfFilters", 0);

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
		System.out.println("Generating filter flow automatically (filters = "+numOfFilters+")...");

		if (numOfFilters <= 0) {
			throw new IllegalStateException(
					"The number of filters must be positive");
		}

		List<Class<?>> classes = null;
		try {
			classes = findWekaClasses();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Generates a random sequence of filters
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
	 *            List of all available weka filters
	 * @param numOfFilters
	 *            Size of the list to generate
	 * @return random list of Weka filters
	 */
	private WekaFilter[] getRandomFilters(List<Class<?>> classes,
			int numOfFilters) {
		WekaFilter[] filterList = new WekaFilter[numOfFilters];

		for (int i = 0; i < numOfFilters; i++) {
			int randomNumber = randInt(0, classes.size());
			try {
				Filter randomFilter = (Filter) classes.get(randomNumber)
						.newInstance();
				System.out.println(randomFilter.toString());
				filterList[i] = new WekaFilter();
				filterList[i].setFilter(randomFilter);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return filterList;
	}

	private void listFilters() {
		List<Class<?>> classes;
		try {
			classes = findWekaClasses();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		for (Class<?> cl : classes) {
			try {
				System.out.println(cl.getName());
			} catch (Throwable t) { // ignore problematic methods and continue
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
		wekaJarPath = URLDecoder.decode(wekaJarPath, "UTF-8").replace("file:",
				"");
		JarFile wekaJar = new JarFile(wekaJarPath);
		Enumeration<JarEntry> contents = wekaJar.entries();
		List<Class<?>> wekaClasses = new ArrayList<Class<?>>(50);
		int badModifiers = Modifier.ABSTRACT | Modifier.INTERFACE
				| Modifier.PRIVATE | Modifier.PROTECTED;
		while (contents.hasMoreElements()) {
			JarEntry entry = contents.nextElement();
			String name = entry.getName();
			if (validClassFile(name)) {
				String className = name.substring(0, name.length() - 6)
						.replaceAll("/", ".");
				Class<?> wekaClass = Class.forName(className);
				if (((wekaClass.getModifiers() & badModifiers) == 0)
						&& weka.filters.Filter.class
								.isAssignableFrom(wekaClass)
						&& hasDefaultConstructor(wekaClass))
					wekaClasses.add(wekaClass);
			}
		}
		wekaJar.close();
		return wekaClasses;
	}

	private boolean hasDefaultConstructor(Class<?> wekaClass) {
		for (Constructor<?> c : wekaClass.getConstructors())
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
	 *            Minimum value
	 * @param max
	 *            Maximum value. Must be greater than min.
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
