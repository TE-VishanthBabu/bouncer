package com.nteligen.hq.dhs.siaft.exceptions;

/**
 * This exception represents that the entity requested to be found from the database was not
 * present.
 */
public class EntityNotFoundException extends DataAccessException
{
  public EntityNotFoundException()
  {
    super();
  }

  public EntityNotFoundException(String message)
  {
    super(message);
  }

  public EntityNotFoundException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
