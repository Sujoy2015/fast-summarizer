package edu.stanford.nlp.parser.lexparser;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.SerializableFunction;
import edu.stanford.nlp.process.WordSegmentingTokenizer;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeReaderFactory;
import edu.stanford.nlp.trees.TreeTransformer;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.international.arabic.ArabicHeadFinder;
import edu.stanford.nlp.trees.international.arabic.ArabicTreeReaderFactory;
import edu.stanford.nlp.trees.international.arabic.ArabicTreebankLanguagePack;
import edu.stanford.nlp.trees.international.arabic.Buckwalter;
import edu.stanford.nlp.trees.tregex.ParseException;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;
import edu.stanford.nlp.util.Filter;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.Pair;


/**
 * A {@link TreebankLangParserParams} implementing class for
 * the Penn Arabic Treebank.  Everything in here should now work with
 * either a Buckwalter or UTF-8 encoding of Arabic words.  Most of it only
 * works with the reduced Bies tag set tags, though some bits were also
 * written to handle the native treebank morphological tags.
 *
 * @author Roger Levy
 * @author Christopher Manning
 */
public class ArabicTreebankParserParams extends AbstractTreebankParserParams {

  /* -- This escaper just changes ( to -LRB- and ) to -RRB-, does nothing else.
     It doesn't seem to be used anywhere, but this functionality (and a bit
     more that is presumably a no-op) could be done by
     process.PTBEscapingProcessor.
  public static class ArabicRomanizedTextEscaper implements Function<List<HasWord>,List<HasWord>> {
    public List<HasWord> apply(List<HasWord> hasWords) {
      List<HasWord> result = new ArrayList<HasWord>(hasWords.size());
      for(HasWord w : hasWords) {
        if(w.word().equals("("))
          result.add(new Word("-LRB-"));
        else if(w.word().equals(")"))
          result.add(new Word("-RRB-"));
        else
          result.add(w);
      }
      return result;
    }
  }
  */

  private String optionsString = "ArabicTreebankParserParams\n";

  private boolean retainNPTmp = false;
  private boolean retainPRD = false;
  private boolean changeNoLabels = false;
  private boolean collinizerRetainsPunctuation = false;
  private Pattern collinizerPruneRegex = null;

  /** True means to delete X top-level constituents. These are usually bylines
   *  like "Riyadh, Jan 27".
   */
  private boolean discardX = false;
  private boolean collapse3LetterPrepositionVariants = false;
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  public ArabicTreebankParserParams() {
    super(new ArabicTreebankLanguagePack());
    initializeAnnotationPatterns();
  }

  public TreeReaderFactory treeReaderFactory() {
    return new ArabicTreeReaderFactory(retainNPTmp, retainPRD,
                                       changeNoLabels, discardX,
                                       collapse3LetterPrepositionVariants);
  }

  @Override
  public MemoryTreebank memoryTreebank() {
    return new MemoryTreebank(treeReaderFactory());
  }

  public DiskTreebank diskTreebank() {
    return new DiskTreebank(treeReaderFactory());
  }

  Class<? extends HeadFinder> headFinderClass = ArabicHeadFinder.class;
  @Override
  public HeadFinder headFinder() {
    try {
      return headFinderClass.newInstance();
    }
    catch(Exception e) {
      System.err.println("Error while instantiating class " + headFinderClass + ": " + e);
      System.err.println("Using ArabicHeadFinder instead.");
      return new ArabicHeadFinder();
    }
  }

  /** Revised collinizer deletes punctuation, which is PUNC or ",",
   *  renames NO_FUNC to NOFUNC, and changes ADVP to PRT.
   *  CDM Apr 2006: The NOFUNC bit is no longer there, and the only thing
   *  this does different to TreeCollinizer is implementing the
   *  collinizerPruneRegex.  Maybe we should add that to TreeCollinizer and
   *  then dispose of this class.
   */
  private static class ArabicCollinizer implements TreeTransformer, Serializable {

    private TreebankLanguagePack tlp;
    private boolean retainPunctuation;
    private Pattern collinizerPruneRegex;
    private Filter<Tree> punctuationRejecter;

    public ArabicCollinizer(TreebankLanguagePack tlp,
                            boolean retainPunctuation, Pattern collinizerPruneRegex) {
      this.tlp = tlp;
      this.retainPunctuation = retainPunctuation;
      this.collinizerPruneRegex = collinizerPruneRegex;
      punctuationRejecter = new PunctuationTreeRejectFilter(tlp);
    }

    public Tree transformTree(Tree t) {
      if (tlp.isStartSymbol(t.value())) {
        t = t.firstChild();
      }
      Tree result = t.deepCopy();
      result = result.prune(new Filter<Tree>() {
        private static final long serialVersionUID = 1669994102700201499L;

        public boolean accept(Tree tree) {
          return collinizerPruneRegex == null || tree.label() == null || ! collinizerPruneRegex.matcher(tree.label().value()).matches();
        }
      });
      if (result == null) {
        return null;
      }
      for (Tree node : result) {
        // System.err.print("ATB collinizer: " + node.label().value()+" --> ");
        if (node.label() != null && ! node.isLeaf()) {
          node.label().setValue(tlp.basicCategory(node.label().value()));
        }
        if (node.label().value().equals("ADVP")) {
          node.label().setValue("PRT");
        }
        // System.err.println(node.label().value());
      }
      if (retainPunctuation) {
        return result;
      } else {
        return result.prune(punctuationRejecter);
      }
    }

    private static final long serialVersionUID = 1L;

  } // end static class ArabicCollinizer


  /** This Filter&lt;Tree&gt; doesn't accept punctuation pre-terminal
   *  (POS tag) nodes according to a TreebankLanguagePack examining the
   *  node's value().
   *  <p>
   *  It seems like this is a facility that should be promoted to the trees
   *  package.
   */
  private static class PunctuationTreeRejectFilter implements Filter<Tree> {

    final Filter<String> punctLabelFilter;

    PunctuationTreeRejectFilter(TreebankLanguagePack tlp) {
      punctLabelFilter = tlp.punctuationTagRejectFilter();
    }

    public boolean accept(Tree tree) {
      return punctLabelFilter.accept(tree.value());
    }

    private static final long serialVersionUID = 1L;

  }


  /**
   * The collinizer eliminates punctuation
   */
  @Override
  public TreeTransformer collinizer() {
    // return new TreeCollinizer(tlp, true, false);
    return new ArabicCollinizer(tlp, collinizerRetainsPunctuation, collinizerPruneRegex);
  }

  /**
   * Stand-in collinizer does nothing to the tree.
   */
  @Override
  public TreeTransformer collinizerEvalb() {
    return collinizer();
  }

  @Override
  public String[] sisterSplitters() {
    return EMPTY_STRING_ARRAY;
  }

  private HashMap<TregexPattern,Function<TregexMatcher,String>> activeAnnotations = new HashMap<TregexPattern,Function<TregexMatcher,String>>();

  @Override
  public Tree transformTree(Tree t, Tree root) {
    StringBuilder newCategory = new StringBuilder(t.label().value());
    for (Map.Entry<TregexPattern,Function<TregexMatcher,String>> e : activeAnnotations.entrySet()) {
      TregexMatcher m = e.getKey().matcher(root);
      if (m.matchesAt(t)) {
        newCategory.append(e.getValue().apply(m));
        //System.out.println("node match " + e.getValue()); //testing
        //t.pennPrint(); //testing
      }
    }
    String newCat = newCategory.toString();
    t.label().setValue(newCat);
    // cdm Mar 2005: the equivalent of the below wasn't being done in the old
    // code, but it really needs to be!
    if (t.isPreTerminal()) {
      HasTag lab = (HasTag) t.label();
      lab.setTag(newCat);
    }
    // t.pennPrint(); //testing
    return t;
  }

  @Override
  public void display() {
    System.err.println(optionsString);
  }

  private Map<String,Pair<TregexPattern,Function<TregexMatcher,String>>> annotationPatterns = new HashMap<String,Pair<TregexPattern,Function<TregexMatcher,String>>>();


  /** This doesn't/can't really pick out genitives,
   *  but just any NP following an NN head.
   */
  private static final String genitiveNodeTregexString = "@NP > @NP $- /^N/";

  private void initializeAnnotationPatterns() {
    try {
      // would need to enrich this to do verb inflection to have a chance?
      // but normally m/f is identical in form
      annotationPatterns.put("-markFem",new Pair<TregexPattern,Function<TregexMatcher, String>>(TregexPattern.compile("__ <<# /p$/"),new SimpleStringFunction("-FEM")));
      annotationPatterns.put("-markGappedVP",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("@VP > @VP $- __ $ /^(?:CC|CONJ)/ !< /^V/"),new SimpleStringFunction("-gappedVP")));
      annotationPatterns.put("-markGappedVPConjoiners",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("/^(?:CC|CONJ)/ $ (@VP > @VP $- __ !< /^V/)"),new SimpleStringFunction("-gappedVP")));
      // gpaPrep works for PCFG (about 0.5%) but not in factored. It kills depDA.
      annotationPatterns.put("-gpAnnotatePrepositions",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("/^(?:IN|PREP)$/ > (__ > __=gp)"),new AddRelativeNodeFunction("^^","gp")));
      annotationPatterns.put("-gpEquivalencePrepositions",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("/^(?:IN|PREP)$/ > (@PP >+(/^PP/) __=gp)"),new AddEquivalencedNodeFunction("^^","gp")));
      annotationPatterns.put("-gpEquivalencePrepositionsVar",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("/^(?:IN|PREP)$/ > (@PP >+(/^PP/) __=gp)"),new AddEquivalencedNodeFunctionVar("^^","gp")));
      annotationPatterns.put("-genitiveMark",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile(genitiveNodeTregexString),new SimpleStringFunction("-genitive")));
      annotationPatterns.put("-markGenitiveParent",new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("@NP < (" + genitiveNodeTregexString + ')'),new SimpleStringFunction("-genitiveParent")));
      // maSdr: this pattern is just a heuristic classification, which matches on
      // various common maSdr pattterns, but probably also matches on a lot of other
      // stuff.  It marks NPs with possible maSdr.
      // Roger's old pattern:
      annotationPatterns.put("-maSdrMark",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^N/ <<# (/^[t\\u062a].+[y\\u064a].$/ > @NN|NOUN|DTNN)"),new SimpleStringFunction("-maSdr")));
      // chris' attempt
      annotationPatterns.put("-maSdrMark2",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^N/ <<# (/^(?:[t\\u062a].+[y\\u064a].|<.{3,}|A.{3,})$/ > @NN|NOUN|DTNN)"),new SimpleStringFunction("-maSdr")));
      annotationPatterns.put("-maSdrMark3",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^N/ <<# (/^(?:[t\\u062a<A].{3,})$/ > @NN|NOUN|DTNN)"),new SimpleStringFunction("-maSdr")));
      annotationPatterns.put("-maSdrMark4",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^N/ <<# (/^(?:[t\\u062a<A].{3,})$/ > (@NN|NOUN|DTNN > (@NP < @NP)))"),new SimpleStringFunction("-maSdr")));
      annotationPatterns.put("-maSdrMark5",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^N/ <<# (__ > (@NN|NOUN|DTNN > (@NP < @NP)))"),new SimpleStringFunction("-maSdr")));
      annotationPatterns.put("-mjjMark",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@JJ|DTJJ < /^m/ $+ @PP ># @ADJP "),new SimpleStringFunction("-mjj")));
      //annotationPatterns.put(markPRDverbString,new Pair<TregexPattern,Function<TregexMatcher,String>>(TregexPattern.compile("/^V[^P]/ > VP $ /-PRD$/"),new SimpleStringFunction("-PRDverb"))); // don't need this pattern anymore, the functionality has been moved to ArabicTreeNormalizer
      // PUNC is PUNC in either raw or Bies POS encoding
      annotationPatterns.put("-splitPUNC",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@PUNC < __=" + AnnotatePunctuationFunction.key),new AnnotatePunctuationFunction()));
      annotationPatterns.put("-markPPwithPPdescendant",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ !< @PP << @PP [ >> @PP | == @PP ]"),new SimpleStringFunction("-inPPdominatesPP")));
      annotationPatterns.put("-markNPwithSdescendant",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ !< @S << @S [ >> @NP | == @NP ]"),new SimpleStringFunction("-inNPdominatesS")));
      annotationPatterns.put("-markContainsVerb",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ << (/^[CIP]?V/ < (__ !< __))"),new SimpleStringFunction("-withV")));
      annotationPatterns.put("-retainNPTmp",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ >># /^NP-TMP/"),new SimpleStringFunction("-TMP")));
      annotationPatterns.put("-markRightRecursiveNP",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ <<- @NP [>>- @NP | == @NP]"),new SimpleStringFunction("-rrNP")));
      annotationPatterns.put("-markBaseNP",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NP !< @NP !< @VP !< @SBAR !< @ADJP !< @ADVP !< @S !< @QP !< @UCP !< @PP"),new SimpleStringFunction("-base")));
      annotationPatterns.put("-markStrictBaseNP",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NP !<  (__ < (__ < __))"),new SimpleStringFunction("-base")));
      annotationPatterns.put("-markContainsSBAR",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ << @SBAR"),new SimpleStringFunction("-withSBAR")));
      annotationPatterns.put("-markPhrasalNodesDominatedBySBAR",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ < (__ < __) >> @SBAR"),new SimpleStringFunction("-domBySBAR")));
      annotationPatterns.put("-markCoordinateNPs",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NP < @CC|CONJ"),new SimpleStringFunction("-coord")));
      annotationPatterns.put("-splitCC",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@CC|CONJ < __=term"),new AddRelativeNodeRegexFunction("-","term", "-*([^-].*)")));
      annotationPatterns.put("-splitIN",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@PP <<# (@IN|PREP < __=term)"), new AddRelativeNodeFunction("-","term")));
      annotationPatterns.put("-markCopularVerbTags",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^V/ < " + copularVerbForms),new SimpleStringFunction("-copular")));
      annotationPatterns.put("-markSBARVerbTags",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^V/ < " + sbarVerbForms),new SimpleStringFunction("-SBARverb")));
      annotationPatterns.put("-markNounNPargTakers",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NN|NNS|NNP|NNPS|DTNN|DTNNS|DTNNP|DTNNPS ># (@NP < @NP)"),new SimpleStringFunction("-NounNParg")));
      annotationPatterns.put("-markNounAdjVPheads",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NN|NNS|NNP|NNPS|JJ|DTJJ|DTNN|DTNNS|DTNNP|DTNNPS ># @VP"),new SimpleStringFunction("-VHead")));
      // a better version of the below might only mark clitic pronouns, but
      // since most pronouns are clitics, let's try this first....
      annotationPatterns.put("-markPronominalNP",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NP < @PRP"),new SimpleStringFunction("-PRP")));
      // try doing coordination parallelism -- there's a lot of that in Arabic (usually the same, sometimes different CC)
      annotationPatterns.put("-markMultiCC", new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ < (@CC $.. @CC)"), new SimpleStringFunction("-multiCC"))); // this unfortunately didn't seem helpful for capturing CC parallelism; should try again
      annotationPatterns.put("-markHasCCdaughter", new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("__ < @CC"), new SimpleStringFunction("-CCdtr")));
      annotationPatterns.put("-markAcronymNP",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("@NP !<  (__ < (__ < __)) < (/^NN/ < /^.$/ $ (/^NN/ < /^.$/)) !< (__ < /../)"), new SimpleStringFunction("-acro")));
      annotationPatterns.put("-markAcronymNN",new Pair<TregexPattern,Function<TregexMatcher,String>>(tregexPatternCompiler.compile("/^NN/ < /^.$/ $ (/^NN/ < /^.$/) > (@NP !<  (__ < (__ < __)) !< (__ < /../))"), new SimpleStringFunction("-acro")));
    } catch (ParseException e) {
      int nth = annotationPatterns.size() + 1;
      String nthStr = (nth == 1) ? "1st": ((nth == 2) ? "2nd": nth + "th");
      System.err.println("Parse exception on " + nthStr + " annotation pattern initialization:" + e);
    }
  }

  // these still need to be turned into UTF-8....
  // Also, for >, coding is different for IBM Arabic vs. raw Arabic.  Yuck.
  private static final String copularVerbForms = "/^(kAn|kAnt|ykwn|sykwn|tkwn|ykn|stkwn|ykwnw|ybdw|tbdw|sybdw|stbdw|bdY|ybdy|tbdy|stbdy|sybdy)$/";
  private static final String sbarVerbForms = "/^(qAl|\\>DAf|AEln|\\>wDH|ymkn|\\>Eln|\\*krt|\\>kd|AElnt|Akd|qAlt|\\>DAft|AfAd|y\\*kr|yjb|\\{Etbr|\\>wDHt|AEtbr|sbq|\\*kr|tAbE|nqlt|SrH|r\\>Y|\\>fAd|AfAdt|yqwl|\\>kdt|\\>Elnt|Akdt|yrY|tEtbr|AEtqd|yEtbr|tfyd|ytwqE|AEtbrt|ynbgy|Tlbt|qrr|ktbt|\\>blg|\\>\\$Ar|ywDH|t\\&kd|Tlb|r\\>t|yEny|nryd|nEtbr|yftrD|k\\$f|\\{Etbrt|AwDH|ytEyn|ykfy|y\\&kd|yErf|ydrk|tZhr|tqwl|tbd\\>|nEtqd|nErf|AErf|Elm|Awrdt|AwDHt|AqtrH|yryd|yErfAn|yElm|ybd\\>tstTyE|tHAwl|tEny|nrY|n\\>ml|)$/";


  private static final TregexPatternCompiler tregexPatternCompiler = new TregexPatternCompiler(new ArabicHeadFinder());

  private static final String markPRDverbString = "-markPRDverbs";

  private static class SimpleStringFunction implements SerializableFunction<TregexMatcher,String> {

    public SimpleStringFunction(String result) {
      this.result = result;
    }

    private String result;

    public String apply(TregexMatcher tregexMatcher) {
      return result;
    }

    @Override
    public String toString() {
      return "SimpleStringFunction[" + result + ']';
    }

    private static final long serialVersionUID = 1L;

  }


  private static class AddRelativeNodeFunction implements SerializableFunction<TregexMatcher,String> {

    private String annotationMark;
    private String key;

    public AddRelativeNodeFunction(String annotationMark, String key) {
      this.annotationMark = annotationMark;
      this.key = key;
    }

    public String apply(TregexMatcher m) {
      return annotationMark + m.getNode(key).label().value();
    }

    @Override
    public String toString() {
      return "AddRelativeNodeFunction[" + annotationMark + ',' + key + ']';
    }

    private static final long serialVersionUID = 1L;

  }


  private static class AddRelativeNodeRegexFunction implements SerializableFunction<TregexMatcher,String> {

    private String annotationMark;
    private String key;
    private Pattern pattern;

    public AddRelativeNodeRegexFunction(String annotationMark, String key, String regex) {
      this.annotationMark = annotationMark;
      this.key = key;
      try {
        this.pattern = Pattern.compile(regex);
      } catch (PatternSyntaxException pse) {
        System.err.println("Bad pattern: " + regex);
        pattern = null;
      }
    }

    public String apply(TregexMatcher m) {
      String val = m.getNode(key).label().value();
      if (pattern != null) {
        Matcher mat = pattern.matcher(val);
        if (m.matches()) {
          val = mat.group(1);
        }
      }
      return annotationMark + val;
    }

    @Override
    public String toString() {
      return "AddRelativeNodeRegexFunction[" + annotationMark + ',' + key + ',' + pattern + ']';
    }

    private static final long serialVersionUID = 1L;

  }


  /** This one only distinguishes VP, S and Other (mainly nominal) contexts.
   *  These seem the crucial distinctions for Arabic true prepositions,
   *  based on raw counts in data.
   */
  private static class AddEquivalencedNodeFunction implements SerializableFunction<TregexMatcher,String> {

    private String annotationMark;
    private String key;

    public AddEquivalencedNodeFunction(String annotationMark, String key) {
      this.annotationMark = annotationMark;
      this.key = key;
    }

    public String apply(TregexMatcher m) {
      String node = m.getNode(key).label().value();
      if (node.startsWith("S")) {
        return annotationMark + 'S';
      } else if (node.startsWith("V")) {
        return annotationMark + 'V';
      } else {
        return "";
      }
    }

    @Override
    public String toString() {
      return "AddEquivalencedNodeFunction[" + annotationMark + ',' + key + ']';
    }

    private static final long serialVersionUID = 1L;

  }


  /** This one only distinguishes VP, S*, A* versus other (mainly nominal) contexts. */
  private static class AddEquivalencedNodeFunctionVar implements SerializableFunction<TregexMatcher,String> {

    private String annotationMark;
    private String key;

    public AddEquivalencedNodeFunctionVar(String annotationMark, String key) {
      this.annotationMark = annotationMark;
      this.key = key;
    }

    public String apply(TregexMatcher m) {
      String node = m.getNode(key).label().value();
      // We also tried if (node.startsWith("V")) [var2] and if (node.startsWith("V") || node.startsWith("S")) [var3]. Both seemed markedly worse than the basic function or this var form (which seems a bit better than the basic equiv option).
      if (node.startsWith("S") || node.startsWith("V") || node.startsWith("A")) {
        return annotationMark + "VSA";
      } else {
        return "";
      }
    }

    @Override
    public String toString() {
      return "AddEquivalencedNodeFunctionVar[" + annotationMark + ',' + key + ']';
    }

    private static final long serialVersionUID = 1L;

  }


  private static class AnnotatePunctuationFunction implements SerializableFunction<TregexMatcher,String> {
    static final String key = "term";
    private static final Pattern endOfSentence = Pattern.compile("^(\\.|\\?.*)$");
    private static final Pattern comma = Pattern.compile(",");
    // private static final Pattern colon = Pattern.compile("^[:;].*$"); // Pattern.compile("^[:;].*$");
    private static final Pattern dash = Pattern.compile("^-.*$");
    private static final Pattern quote = Pattern.compile("^\"$");
    // private static final Pattern slash = Pattern.compile("^\\/$");
    // private static final Pattern percent = Pattern.compile("^\\%$");
    // private static final Pattern ellipses= Pattern.compile("^\\.\\.\\.$");
    private static final Pattern lrb = Pattern.compile("^-LRB-$");
    private static final Pattern rrb = Pattern.compile("^-RRB-$");

    public String apply(TregexMatcher m) {
      String punc = m.getNode(key).label().value();
      if (endOfSentence.matcher(punc).matches())
        return "-eos";
      if (comma.matcher(punc).matches())
        return "-comma";
      // if (colon.matcher(punc).matches())
      //   return "-colon";
      // checking for lrb/rrb must precede dash check!!!
      if (lrb.matcher(punc).matches())
        return "-lrb";
      if (rrb.matcher(punc).matches())
        return "-rrb";
      if (dash.matcher(punc).matches())
        return "-dash";
      if (quote.matcher(punc).matches())
        return "-quote";
      //if(slash.matcher(punc).matches())
      //  return "-slash";
      //if(percent.matcher(punc).matches())
      // return "-percent";
      //if(ellipses.matcher(punc).matches())
      //  return "-ellipses";
      return "";
    }

    @Override
    public String toString() {
      return "AnnotatePunctuationFunction";
    }

    private static final long serialVersionUID = 1L;

  } // end class AnnotatePunctuationFunction


  /** Some options for setOptionFlag:
   *
   * <p>
   * <code>-retainNPTmp</code> retain temporal NP marking on NPs.
   * <code>-markGappedVP</code> marked gapped VPs.
   * <code>-collinizerRetainsPunctuation</code> does what it says.
   * </p>
   *
   * @param args flag arguments (usually from commmand line
   * @param i index at which to begin argument processing
   * @return Index in args array after the last processed index for option
   */
  @Override
  public int setOptionFlag(String[] args, int i) {
    //System.out.println("Setting option flag: "  + args[i]);

    //lang. specific options
    boolean didSomething = false;
    if (annotationPatterns.keySet().contains(args[i])) {
      Pair<TregexPattern,Function<TregexMatcher,String>> p = annotationPatterns.get(args[i]);
      activeAnnotations.put(p.first(),p.second());
      optionsString += "Option " + args[i] + " added annotation pattern " + p.first() + " with annotation " + p.second() + '\n';
      didSomething = true;
    } else if (args[i].equals("-retainNPTmp")) {
      optionsString += "Retaining NP-TMP marking.\n";
      retainNPTmp = true;
      didSomething = true;
    } else if (args[i].equals("-discardX")) {
      optionsString += "Discarding X trees.\n";
      discardX = true;
      didSomething = true;
    } else if (args[i].equals("-changeNoLabels")) {
      optionsString += "Change no labels.\n";
      changeNoLabels = true;
      didSomething = true;
    } else if (args[i].equals("-collapse3LetterPrepositionVariants")) {
      optionsString += "Collapse 3 Letter Preposition Variants\n";
      collapse3LetterPrepositionVariants = true;
      didSomething = true;
    } else if (args[i].equals(markPRDverbString)) {
      optionsString += "Mark PRD.\n";
      retainPRD = true;
      didSomething = true;
    } else if (args[i].equals("-collinizerRetainsPunctuation")) {
      optionsString += "Collinizer retains punctuation.\n";
      collinizerRetainsPunctuation = true;
      didSomething = true;
    } else if (args[i].equals("-collinizerPruneRegex")) {
      optionsString += "Collinizer prune regex: " + args[i+1] + '\n';
      collinizerPruneRegex = Pattern.compile(args[i+1]);
      i++;
      didSomething = true;
    } else if (args[i].equals("-hf")) {
      try {
        headFinderClass = Class.forName(args[i+1]).asSubclass(HeadFinder.class); // ensures it's a true HeadFinder.
        optionsString += "HeadFinder class: " + args[i+1] + '\n';
      }
      catch(ClassNotFoundException e) {
        System.err.println("Error -- can't find HeadFinder class" + args[i+1]);
      }
      i++; // 2 args
      didSomething = true;
    } else if (args[i].equals("-arabicFactored")) {
      String[][] opts = { {"-discardX"}, {"-markNounNPargTakers"}, {"-genitiveMark"},
              {"-splitPUNC"}, {"-markContainsVerb"}, {"-splitCC"}, {"-markContainsSBAR"} };
      for (String[] opt : opts) {
        setOptionFlag(opt, 0);
      }
      didSomething = true;
    } else if (args[i].equals("-arabicTokenizerModel")) {
      String modelFile = args[i+1];
      try {
        WordSegmenter aSeg = (WordSegmenter) Class.forName("edu.stanford.nlp.wordseg.ArabicSegmenter").newInstance();
        aSeg.loadSegmenter(modelFile);
        System.out.println("aSeg=" + aSeg);
        TokenizerFactory<Word> aTF = WordSegmentingTokenizer.factory(aSeg);
        ((ArabicTreebankLanguagePack) treebankLanguagePack()).setTokenizerFactory(aTF);
      } catch (RuntimeIOException ex) {
        System.err.println("Couldn't load ArabicSegmenter " + modelFile);
        ex.printStackTrace();
      } catch (Exception e) {
        System.err.println("Couldn't instantiate segmenter: edu.stanford.nlp.wordseg.ArabicSegmenter");
        e.printStackTrace();
      }
      i++; // 2 args
      didSomething = true;
    }
    if (didSomething) {
      i++;
    }
    return i;
  }

  /**
   * Return a default sentence for the language (for testing).
   * This test sentence is Buckwalter encoded, so it only works if you're
   * using a Buckwalter encoding parser.
   */
  public Sentence<Word> defaultTestSentence() {
    return Sentence.toSentence("w", "lm", "tfd", "mElwmAt", "En",
                                "ADrAr", "Aw", "DHAyA", "HtY", "AlAn", ".");
  }

  private static final long serialVersionUID = 1L;


  /**
   * Loads Arabic Treebank files from the first argument and prints all the trees below length specified by second arg.
   * @param args Command line arguments, as above
   */
  public static void main(String[] args) {
    int maxLength = Integer.parseInt(args[1]);
    boolean b2a = false;
    TreebankLangParserParams tlpp = new ArabicTreebankParserParams();
    if (args[2].equals("-b2a")) {
      b2a = true;
    } else {
      tlpp.setOptionFlag(args,2);
    }
    DiskTreebank trees = tlpp.diskTreebank();
    trees.loadPath(args[0]);
    PrintWriter pw = tlpp.pw();
    TreeTransformer mapper = new TreeTransformer() {

        private Buckwalter buck = new Buckwalter();

        public Tree transformTree(Tree t) {
          for (Tree tr : t) {
            if (tr.isLeaf()) {
              //System.err.println("Changing " + tr.value() + " to " +
              //                   buck.buckwalterToUnicode(tr.value()));
              tr.setValue(buck.buckwalterToUnicode(tr.value()));
            }
          }
          return t;
        }
      };

    for (Tree t : trees) {
      if (t.yield().size() <= maxLength) {
        pw.println(t);
        if (b2a) {
          mapper.transformTree(t).pennPrint(pw);
        }
      }
    }
  }

  /**
   * Returns a lexicon for Arabic.  At the moment this is just a BaseLexicon.
   * @return A lexicon
   */
  @Override
  public Lexicon lex() {
    return new BaseLexicon();
  }

  /**
   * Returns a lexicon for Arabic.  At the moment this is just a BaseLexicon.
   * @param op Lexicon options
   * @return A Lexicon
   */
  @Override
  public Lexicon lex(Options.LexOptions op) {
    if(op.uwModel == null) {
      op.uwModel = "edu.stanford.nlp.parser.lexparser.ArabicUnknownWordModel";
    }
    return new BaseLexicon(op);
  }

}
