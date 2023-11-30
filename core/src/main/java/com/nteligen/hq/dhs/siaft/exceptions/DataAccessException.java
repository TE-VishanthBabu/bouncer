package com.nteligen.hq.dhs.siaft.exceptions;

/**
 * This exception represents that a problem occurred when trying to access data from a source.
 */
public class DataAccessException extends PersistenceException
{
  public DataAccessException()
  {
    super();
  }

  public DataAccessException(String message)
  {
    super(message);
  }

  public DataAccessException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
