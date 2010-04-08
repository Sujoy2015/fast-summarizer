package edu.stanford.nlp.util;

/**
 * Just a single integer
 *
 * @author Kristina Toutanova (kristina@cs.stanford.edu)
 */

public class IntUni extends IntTuple {
  public IntUni() {
    elements = new int[1];
  }


  public IntUni(int src) {
    elements = new int[1];
    elements[0] = src;
  }


  public int getSource() {
    return elements[0];
  }

  public void setSource(int src) {
    elements[0] = src;
  }


  @Override
  public int hashCode() {
    return (elements[0]);
  }


  @Override
  public IntTuple getCopy() {
    IntUni nT = new IntUni(elements[0]);
    return nT;
  }

  public void add(int val) {
    elements[0] += val;
  }

  // Automatically generated by Eclipse
  private static final long serialVersionUID = -7182556672628741200L;
}
