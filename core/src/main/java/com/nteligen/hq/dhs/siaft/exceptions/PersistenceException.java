package com.nteligen.hq.dhs.siaft.exceptions;

/**
 * This exception represents that a problem occurred when trying to access data from a source.
 */
public class PersistenceException extends SIAFTException
{
  public PersistenceException()
  {
    super();
  }

  public PersistenceException(String message)
  {
    super(message);
  }

  public PersistenceException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
