package com.nteligen.hq.dhs.siaft.dao;

import java.util.Date;

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

import lombok.Data;

@Data
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
}
