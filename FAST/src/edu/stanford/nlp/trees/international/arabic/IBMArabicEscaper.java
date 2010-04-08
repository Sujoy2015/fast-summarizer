package edu.stanford.nlp.trees.international.arabic;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;
import java.io.*;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.StringUtils;


/**
 * This escaper deletes the '#' and '+' symbols that the IBM segmenter uses
 * to mark prefixes and suffixes, since they're not present in the Penn
 * Arabic treebank materials (though later we might try adding them), and
 * escapes the parenthesis characters.
 *
 * @author Christopher Manning
 */
public class IBMArabicEscaper implements Function<List<HasWord>, List<HasWord>> {

  private static final Pattern pEnt = Pattern.compile("\\$[a-z]+_\\((.*?)\\)");
  private static final Pattern presForms = Pattern.compile("[\uFB50-\uFDFF\uFE70-\uFEFE]");
  private static final Pattern extendedArabic = Pattern.compile("[\u063B-\u063F\u0671-\u06FF\u0750-\u077F]");
  private static final Pattern alefVariants = Pattern.compile("[\u0622\u0623\u0625]");
  private static final Pattern pAM = Pattern.compile("\u0649");
  private static final Pattern pDel = Pattern.compile("[\u064B-\u0655\u0670]");
  private static final Pattern pTatweel = Pattern.compile("\u0640"); // only applied to words of length > 1
  private static final Pattern pYaaHamza = Pattern.compile("\u064A\u0621");


  private boolean warnedPresentationForms; // = false;
  private boolean warnedExtendedArabic; // = false;
  private boolean warnedEntityEscaping; // = false
  private boolean warnedNormalization; // = false;
  private boolean warnedDeletion; // = false;
  private boolean warnedProcliticEnclitic; // = false


  private String escapeString(String w) {
    if ( ! warnedPresentationForms) {
      Matcher m1 = presForms.matcher(w);
      if (m1.find()) {
        System.err.println("IBMArabicEscaper Warning: encountering Arabic presentation form characters which are NOT mapped but just treated as unknown characters: " + w);
        warnedPresentationForms = true;
      }
    }
    if ( ! warnedExtendedArabic) {
      Matcher m3 = extendedArabic.matcher(w);
      if (m3.find()) {
        System.err.println("IBMArabicEscaper Warning: encountering Arabic presentation form characters which are NOT mapped but just treated as unknown characters: " + w);
        warnedExtendedArabic = true;
      }
    }
    Matcher mAlef = alefVariants.matcher(w);
    if (mAlef.find()) {
      if ( ! warnedNormalization) {
        System.err.println("IBMArabicEscaper Note: equivalence classing certain characters, such as Alef with madda/hamza, e.g., in: " + w);
        warnedNormalization = true;
      }
      // hamza normalization: maddah-on-alef, hamza-on-alef, hamza-under-alef mapped to bare alef
      w = mAlef.replaceAll("\u0627");
    }
    Matcher mAM = pAM.matcher(w);
    if (mAM.find()) {
      if ( ! warnedNormalization) {
        System.err.println("IBMArabicEscaper Note: equivalence classing certain characters, such as Alef with madda/hamza, e.g., in: " + w);
        warnedNormalization = true;
      }
      // 'alif maqSuura mapped to yaa
      w = mAM.replaceAll("\u064A");
    }
    Matcher mYH = pYaaHamza.matcher(w);
    if (mYH.find()) {
      if ( ! warnedNormalization) {
        System.err.println("IBMArabicEscaper Note: equivalence classing certain characters, such as Alef with madda/hamza, e.g., in: " + w);
        warnedNormalization = true;
      }
      // replace yaa followed by hamza with hamza on kursi (yaa)
      w = mYH.replaceAll("\u0626");
    }
    Matcher mDel = pDel.matcher(w);
    if (mDel.find()) {
      if ( ! warnedDeletion) {
        System.err.println("IBMArabicEscaper Note: deleting certain characters, such as tatweel, fatHa, kasra, damma, e.g., in: " + w);
        warnedDeletion = true;
      }
      // fatHatayn, Dammatayn, kasratayn, fatHa, Damma, kasra, shaddah, sukuun, maddah above and dagger alef (are deleted)
      w = mDel.replaceAll("");
    }
    w = StringUtils.tr(w, "\u060C\u061B\u061F\u066A\u066B\u066C\u066D\u06D4\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669\u0966\u0967\u0968\u0969\u096A\u096B\u096C\u096D\u096E\u096F\u2013\u2014\u0091\u0092\u2018\u2019\u0093\u0094\u201C\u201D",
                          ",;%.,*.01234567890123456789--''''\"\"\"\"");

    int wLen = w.length();
    if (wLen > 1) {  // only for two or more letter words
      Matcher m2 = pEnt.matcher(w);
      if (m2.matches()) {
        if ( ! warnedEntityEscaping) {
          System.err.println("IBMArabicEscaper Note: escaping IBM MT-style entities: " + m2.group(0) + " --> " + m2.group(1));
          warnedEntityEscaping = true;
        }
        w = m2.replaceAll("$1");
      } else if (w.charAt(0) == '+') {
        if ( ! warnedProcliticEnclitic) {
          warnedProcliticEnclitic = true;
          System.err.println("IBMArabicEscaper Note: removing IBM MT-style proclitic/enclitic indicators, e.g., on " + w);
        }
        w = w.substring(1);
      // doesn't seem that ther are inital - sign markers in data.
      // } else if (w.charAt(0) == '-' && w.charAt(1) != '-') {
      //  w = w.substring(1);
      } else if (w.charAt(wLen - 1) == '#') {
        if ( ! warnedProcliticEnclitic) {
          warnedProcliticEnclitic = true;
          System.err.println("IBMArabicEscaper Note: removing IBM MT-style proclitic/enclitic indicators, e.g., on " + w);
        }
        w = w.substring(0, wLen - 1);
      }
      Matcher mTatweel = pTatweel.matcher(w);
      if (mTatweel.find()) {
        if ( ! warnedDeletion) {
          System.err.println("IBMArabicEscaper Note: deleting certain characters, such as tatweel, fatHa, kasra, damma, e.g., in: " + w);
          warnedDeletion = true;
        }
        // tatweel deleted in words of length > 1
        w = mTatweel.replaceAll("");
      }
    } else {
      if (w.equals("(")) {
        w = "-LRB-";
      } else if (w.equals(")")) {
        w = "-RRB-";
      } else if (w.equals("+")) {
        w = "-PLUS-";  // This was newly added in ATBp3v3
      }
    }
    return w;
  }


  /** <i>Note:</i> At present this clobbers the input list items.
   *  This should be fixed.
   */
  public List<HasWord> apply(List<HasWord> arg) {
    List<HasWord> ans = new ArrayList<HasWord>(arg);
    for (HasWord wd : ans) {
      wd.setWord(escapeString(wd.word()));
    }
    return ans;
  }

  /** This main method preprocesses one-sentence-per-line input, making the
   *  same changes as the Function.
   *
   *  @param args A list of filenames.  The files must be UTF-8 encoded.
   *  @throws IOException If there are any issues
   */
  public static void main(String[] args) throws IOException {
    IBMArabicEscaper escaper = new IBMArabicEscaper();
    for (String arg : args) {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arg), "UTF-8"));
      String outFile = arg + ".sent";
      PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8")));
      for (String line ; (line = br.readLine()) != null; ) {
        String[] words = line.split("\\s+");
        for (int i = 0; i < words.length; i++) {
          String w = escaper.escapeString(words[i]);
          pw.print(w);
          if (i != words.length - 1) {
            pw.print(" ");
          }
        }
        pw.println();
      }
      br.close();
      pw.close();
    }
  }

}
