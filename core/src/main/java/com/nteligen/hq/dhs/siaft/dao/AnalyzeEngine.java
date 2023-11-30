package com.nteligen.hq.dhs.siaft.dao;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "AnalyzeEngines")
public class AnalyzeEngine
{
  @Id
  @Column(name = "PK_AnalyzeEngine_ID")
  private long analyzeEngineId;

  @Column(name = "EngineName")
  private String engineName;

  @CreationTimestamp
  @Column(name = "Date_Created")
  private Date dateCreated;

  @Column(name = "Date_Updated")
  private Date dateUpdated;
}
