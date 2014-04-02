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

import java.util.Arrays;

import weka.core.Instances;
import adams.core.ClassLister;
import adams.core.base.BaseRegExp;
import adams.core.base.BaseString;
import adams.core.io.PlaceholderFile;
import adams.core.option.OptionUtils;
import adams.data.conversion.SpreadSheetToWekaInstances;
import adams.data.io.input.CsvSpreadSheetReader;
import adams.data.spreadsheet.SpreadSheet;
import adams.env.Environment;
import adams.flow.control.Flow;
import adams.flow.core.AbstractActor;
import adams.flow.sink.Display;
import adams.flow.source.StringConstants;

public class MyTransformer extends AbstractActorTemplate {

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
		m_OptionManager.add("csvReader", "csvReader",
				new CsvSpreadSheetReader());
	}

	/**
	 * Sets the training file to load.
	 * 
	 * @param value
	 *            the file
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
	 *            the csv reader
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
		// 1. Read file
		// TODO Extend to other types of files
		SpreadSheet sheet = m_CsvReader.read(m_TrainingFile);

		// Converting to Weka format to use Weka methods
		SpreadSheetToWekaInstances m_Conversion = new SpreadSheetToWekaInstances();
		m_Conversion.setInput(sheet);
		String out = m_Conversion.convert();
		if (out != null)
			return null; // TODO throw conversion error

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
						+ Arrays.toString(numMissingValuesByColumn)) });
		Display report = new Display();

		listClasses();

		// 3. Generate workflow based on the data characteristics
		Flow flow = new Flow();
		flow.setActors(new AbstractActor[] { characteristics, report });
		return flow;
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
			String match = ".*\\.transformer\\..*";
			if (match == null)
				match = BaseRegExp.MATCH_ALL;
			BaseRegExp regexp = new BaseRegExp(match);

			// allow empty class hierarchies?
			boolean allowEmpty = false;

			// superclass
			String[] superclasses;
			String sclass = "adams.flow.core.AbstractActor";
			if (sclass == null)
				superclasses = m_ClassLister.getSuperclasses();
			else
				superclasses = new String[] { sclass };

			// list them
			for (String superclass : superclasses) {
				cls = Class.forName(superclass);
				String[] classnames = m_ClassLister.getClassnames(cls);
				if ((classnames.length > 0) || allowEmpty) {
					System.out.println("--> " + superclass);
					for (String classname : classnames) {
						if (regexp.isMatch(classname))
							System.out.println(classname);
					}
					System.out.println();
				}
			}
			System.out.println("ClassLister ends");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
