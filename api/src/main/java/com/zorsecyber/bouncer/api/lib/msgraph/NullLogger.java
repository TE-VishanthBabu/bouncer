package com.zorsecyber.bouncer.api.lib.msgraph;

import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.logger.LoggerLevel;

import javax.annotation.Nonnull;

/**
* The default logger for the service client
*/
public class NullLogger implements ILogger {

 /**
  * The logging level
  */
 private LoggerLevel level = LoggerLevel.ERROR;

 private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

 /**
  * Sets the logging level of this logger
  *
  * @param level the level to log at
  */
 public void setLoggingLevel(@Nonnull final LoggerLevel level) {
     LOGGER.info("Setting logging level to " + level);
     this.level = Objects.requireNonNull(level, "parameter level cannot be null");
 }

 /**
  * Gets the logging level of this logger
  *
  * @return the level the logger is set to
  */
 @Nonnull
 public LoggerLevel getLoggingLevel() {
     return level;
 }

 /**
  * Creates the tag automatically
  *
  * @return the tag for the current method
  * Sourced from https://gist.github.com/eefret/a9c7ac052854a10a8936
  */
 @Nullable
 private String getTag() {
     try {
         final StringBuilder sb = new StringBuilder();
         final int callerStackDepth = 4;
         final String className = Thread.currentThread().getStackTrace()[callerStackDepth].getClassName();
         sb.append(className.substring(className.lastIndexOf('.') + 1));
         sb.append("[");
         sb.append(Thread.currentThread().getStackTrace()[callerStackDepth].getMethodName());
         sb.append("] - ");
         sb.append(Thread.currentThread().getStackTrace()[callerStackDepth].getLineNumber());
         return sb.toString();
     } catch (final Exception ex) {
         LOGGER.warning(ex.getMessage());
     }
     return null;
 }

 /**
  * Logs a debug message
  *
  * @param message the message
  */
 @Override
 public void logDebug(@Nonnull final String message) {
 }

 /**
  * Logs an error message with throwable
  *
  * @param message   the message
  * @param throwable the throwable
  */
 @Override
 public void logError(@Nonnull final String message, @Nonnull final Throwable throwable) {
 }
}
