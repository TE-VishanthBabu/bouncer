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

/**
 * This class represents a file that was not processed through the SIAFT system
 * of a particular reason.
 */
@Data
@Entity
@Table(name = "UnprocessedFiles")
public class UnprocessedFile
{
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "PK_UnprocessedFiles_ID")
  private long unprocessedFileId;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "FK_FileAttributes_ID")
  private FileAttribute fileAttribute;

  @Column(name = "Unprocessed")
  private boolean unprocessed;

  @Column(name = "UnprocessedReason")
  private String unprocessedReason;

  @CreationTimestamp
  @Column(name = "Date_Created")
  private Date dateCreated;

  @Column(name = "Date_Updated")
  private Date dateUpdated;

  public UnprocessedFile()
  {
  }

  /**
  * UnprocessedFile entry loader.
  *
  */
  public UnprocessedFile(FileAttribute fileAttribute, boolean unprocessed, String unprocessedReason)
  {
    this.fileAttribute = fileAttribute;
    this.unprocessed = unprocessed;
    this.unprocessedReason = unprocessedReason;
  }
}
