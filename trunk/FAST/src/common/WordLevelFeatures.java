package common;

import java.util.ArrayList;
import edu.stanford.nlp.trees.Tree;

public class WordLevelFeatures {
	
	/* Gautam */
	public static int computeTermFrequency( Tree t ) 
	{
		return 1;
	}
	
	
	/* Alhaad */
	public static int computePartOfSpeechScore ( Tree t )
	{
		return 1;
	}
	
	/* Mayank */
	public static int computeFamiliarityScore ( Tree t )
	{
		return 1;
	}
	
	/* Manaal */
	public static int computNamedEntityScore ( Tree t )
	{
		return 1;
	}
	
	/* Manaal */
	/**
	 * @param t : the leaf node containing the word for which the score has to be computed
	 * @param wordsInHeadings : the words that are there in headings of the text
	 */
	public static int computeHeadingScore ( String str , ArrayList<String> wordsInHeadings )
	{		
		return 1;
	}

}
