package com.zorsecyber.bouncer.api.dao;

import java.util.Date;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Sanitize")
public class Sanitize
{
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "PK_Sanitize_ID")
  private long sanitizeId;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "FK_FileAttributes_ID")
  private FileAttribute fileAttribute;

  @ManyToOne (cascade = CascadeType.ALL)
  @JoinColumn(name = "FK_SanitizeEngineID")
  private SanitizeEngine sanitizeEngine;

//  @ManyToMany(cascade = CascadeType.ALL)
//  @JoinTable(name = "Dynamic_Analysis_Sanitizations",
//             joinColumns = {@JoinColumn(name = "FK_Sanitize_ID", referencedColumnName =
//                  "PK_Sanitize_ID")},
//             inverseJoinColumns = {@JoinColumn(name = "FK_Dynamic_Analysis_ID",
//             referencedColumnName = "PK_Dynamic_Analysis_ID")}
//            )
//  private Set<DynamicAnalysis> dynamicAnalysisSet;

  @Column(name = "Results")
  private String results;

  @Column(name = "MD5")
  private String md5;

  @Column(name = "FileType")
  private String fileType;

  @CreationTimestamp
  @Column(name = "Date_Created")
  private Date dateCreated;

  @Column(name = "Date_Updated")
  private Date dateUpdated;

  @Override
  @SuppressWarnings({"checkstyle:whitespacearound"})
  public String toString()
  {
    return "Sanitize{"
           + "sanitizeId=" + sanitizeId
           + ", fileAttribute=" + fileAttribute
           + ", sanitizeEngine=" + sanitizeEngine
           + ", results='" + results + '\''
           + ", md5='" + md5 + '\''
           + ", fileType='" + fileType + '\''
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
    if (!(other instanceof Sanitize))
    {
      return false;
    }
    Sanitize sanitize = (Sanitize) other;
    return sanitizeId == sanitize.sanitizeId
           && Objects.equals(fileAttribute, sanitize.fileAttribute)
           && Objects.equals(sanitizeEngine, sanitize.sanitizeEngine)
           && Objects.equals(results, sanitize.results)
           && Objects.equals(md5, sanitize.md5)
           && Objects.equals(fileType, sanitize.fileType)
           && Objects.equals(dateCreated, sanitize.dateCreated)
           && Objects.equals(dateUpdated, sanitize.dateUpdated);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(sanitizeId, fileAttribute, sanitizeEngine, results, md5, fileType,
            dateCreated, dateUpdated);
  }
}
