package common;

import java.util.ArrayList;
import edu.stanford.nlp.trees.Tree;

public class WordLevelFeatures {
	
	public static int computeTermFrequency( Tree t ) 
	{
		return 1;
	}
	
	
	public static int computePartOfSpeechScore ( Tree t )
	{
		return 1;
	}
	
	public static int computeFamiliarityScore ( Tree t )
	{
		return 1;
	}
	
	public static int computNamedEntityScore ( Tree t )
	{
		return 1;
	}
	
	/**
	 * @param t : the leaf node containing the word for which the score has to be computed
	 * @param wordsInHeadings : the words that are there in headings of the text
	 */
	public static int computeHeadingScore ( String str , ArrayList<String> wordsInHeadings )
	{		
		return 1;
	}

}
