package common;

/** TODO
 * 1. Explanation for the arguments of constructor
 * 2. Discussion about the formula for the final score computation (Multiplication doesn't seem like a good idea)
 */
import java.util.List;
import java.util.ArrayList;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;

public class SentenceScorer {
	 
	/**
	 * @param sentence : 
	 * @param positionInText : relative position of a sentence in the text normalized to [0,1];
	 * @param parseTree : parseTree is also an input (for optimization , i will explain )
	 */
	public SentenceScorer ( String sentence , double positionInText , 
							Tree parseTree , ArrayList<String> wordsInHeadings )
	{
		this.sentence = sentence;
		this.positionInText = positionInText;
		this.parseTree = parseTree;
		this.wordsInHeadings = wordsInHeadings;
	}
	
	/**
	 * Computes the score of a sentence and saves in the variable 'score'
	 * Also returns the computed value
	 * @return a relative score in double
	 */
	public double computeScore ()
	{
		// all methods to be called from here 
		// and the value saved in the instance variable score
		// The scheme is very raw, just for coherence
		boolean verbPresent = isVerbPresent();
		int lengthOfSentence = computeLengthOfSentence();
		
		
		List<Tree> leafNodes = parseTree.getLeaves();
		int sum = 0;
		for ( Tree t : leafNodes ) {
			
			// Extract the word that the leaf node stores 
			String str = t.label().value(); 
			
			System.out.println(str); 
			
			int termFrequencyScore = WordLevelFeatures.computeTermFrequency(t);
			int lengthOfWordScore = str.length();
			int partOfSpeechScore = WordLevelFeatures.computePartOfSpeechScore(t);
			
			
		/* @ Mayank, I don't know whether you require the node in the parse tree or only the string 
		 * so feel free to change it. At present I am sending the leaf-node for the word to the method, 
		 * but if you only want the word, change the type to 'String' instead of 'Tree'
		 */
			int familiarityScore = WordLevelFeatures.computeFamiliarityScore(t);
			int namedEntityScore = WordLevelFeatures.computNamedEntityScore(t);
			int headingScore = WordLevelFeatures.computeHeadingScore(str, wordsInHeadings);
			
			/* This multiplication formula to be suitable due to a no. of reasons but 
			 * I have still put it this way as of now
			 */
			int wordScore = termFrequencyScore * lengthOfWordScore * partOfSpeechScore *
							familiarityScore * namedEntityScore * headingScore;
			/* Garbage for now */
			sum = sum + wordScore;
		}
		return (double) sum + lengthOfSentence;
	}
	
	/** Coding by Alhaad */
	public boolean isVerbPresent ()
	{
		return false;
	}
	
	/** Gautam */
	public int computeLengthOfSentence ()
	{
		return 1;
	}
	
	
	/** Some Accessor / Mutator Methods */
	public double getScore () { return score; }
	
	public void setSentence ( String sentence ) { this.sentence = sentence; }
	public String getSentence () { return sentence; }
	
	public void setPositionInText ( double positionInText ) { this.positionInText = positionInText; }
	public double getPositionInText () { return positionInText; }
	
	public void setParseTree ( Tree parseTree ) { this.parseTree = parseTree; }
	public Tree getParseTree () { return parseTree; }
	
	public void setWordsInHeadings ( ArrayList<String> wordsInHeadings ) { this.wordsInHeadings = wordsInHeadings; }
	public ArrayList<String> getWordsInHeadings () { return wordsInHeadings; } 
	
	
	/** A dummy main method */
	public static void main ( String[] args )
	{
		LexicalizedParser lp = new LexicalizedParser("englishPCFG.ser.gz");
		String parseInput = "This is just a test input";
		Tree parse = (Tree) lp.apply(parseInput);
		SentenceScorer scorer = new SentenceScorer(parseInput, 0.5, parse, null);
		double score = scorer.computeScore();
		
		
	}
	
	private String sentence; 
	private double positionInText;
	private double score;
	private Tree parseTree;
	private ArrayList<String> wordsInHeadings;
}


