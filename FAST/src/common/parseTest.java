package common;
	
import java.util.*;
import java.io.StringReader;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;

    
public class ParseTest{
	public static void main(String[] args){
		LexicalizedParser lp = new LexicalizedParser("englishPCFG.ser.gz");
		String parseInput = "This is just a test input. This contains three sentences. This is the third sentence.";
		StringReader parseInputReader = new StringReader(parseInput);
		DocumentPreprocessor preInput = new DocumentPreprocessor();
		List sentences=preInput.getSentencesFromText(parseInputReader);
		for(int i=0;i<sentences.size();i++){
			Tree parse = (Tree) lp.apply(sentences.toArray()[i]);
			parse.pennPrint();
			System.out.println();
		}
	}
}