package com.zorsecyber.bouncer.api.dao;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SanitizeEngines")
public class SanitizeEngine
{
  @Id
  @Column(name = "PK_SanitizeEngine_ID")
  private long sanitizeEngineId;

  @Column(name = "EngineName")
  private String engineName;

  @CreationTimestamp
  @Column(name = "Date_Created")
  private Date dateCreated;

  @Column(name = "Date_Updated")
  private Date dateUpdated;

  @Override
  public String toString()
  {
    return "SanitizeEngine{"
           + "sanitizeEngineId=" + sanitizeEngineId
           + ", engineName='" + engineName + '\''
           + ", dateCreated=" + dateCreated
           + ", dateUpdated=" + dateUpdated
           + '}';
  }

  @Override
  public boolean equals(Object other)
  {
    if (this == other)
    {
      return true;
    }
    if (!(other instanceof SanitizeEngine))
    {
      return false;
    }
    SanitizeEngine that = (SanitizeEngine) other;
    return sanitizeEngineId == that.sanitizeEngineId
           && Objects.equals(engineName, that.engineName)
           && Objects.equals(dateCreated, that.dateCreated)
           && Objects.equals(dateUpdated, that.dateUpdated);
  }

  @Override
  public int hashCode()
  {

    return Objects.hash(sanitizeEngineId, engineName, dateCreated, dateUpdated);
  }
}
