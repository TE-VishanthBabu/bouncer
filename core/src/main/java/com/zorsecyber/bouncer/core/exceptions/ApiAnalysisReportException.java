package com.zorsecyber.bouncer.core.exceptions;

/**
 * This exception represents that a problem occurred during file analysis.
 */
public class ApiAnalysisReportException extends RuntimeException
{
  public ApiAnalysisReportException()
  {
    super();
  }

  public ApiAnalysisReportException(String message)
  {
    super(message);
  }

  public ApiAnalysisReportException(String message, Throwable cause)
  {
    super(message, cause);
  }
  
  public ApiAnalysisReportException(final Throwable cause) {
      super(cause);
  }
}
