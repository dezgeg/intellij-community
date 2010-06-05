package com.jetbrains.python.toolbox;

import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * Encapsulates a value which may be either defined or not.
 * Useful when null must be a defined value.
 * Instances are immutable.
 * <br/>
 * User: dcheryasov
 * Date: Jun 5, 2010 3:52:59 PM
 */
public class Maybe<T> {
  private boolean myIsDefined;
  private T myValue;

  /**
   * @return true iff this instance has a defined value.
   */
  public boolean isDefined() {
    return myIsDefined;
  }


  /**
   * @return value if it is defined, or null.
   */
  @Nullable
  public T valueOrNull() {
    if (myIsDefined) return myValue;
    else return null;
  }

  /**
   * @return value if it is defined; else throws a NoSuchElementException.
   */
  public T value() {
    if (myIsDefined) return myValue;
    else throw new NoSuchElementException("Accessing undefined value of Maybe");
  }

  /**
   * Creates a defined instance.
   * @param value what to store.
   */
  public Maybe(T value) {
    myValue = value;
    myIsDefined = true;
  }

  /**
   * Creates an undefined instance.
   */
  public Maybe() {
    myValue = null;
    myIsDefined = false;
  }

  @Override
  public String toString() {
    if (myIsDefined) {
      if (myValue == null) return "?(null)";
      else return "?(" + myValue.toString() + ")";
    }
    else return "?_";
  }
}
