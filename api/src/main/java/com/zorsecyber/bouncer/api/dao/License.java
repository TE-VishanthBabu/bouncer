package com.zorsecyber.bouncer.api.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Licenses")
public class License {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_License_ID")
    private Integer licenseId;
	
    @OneToOne
    @JoinColumn(name = "FK_User_ID", referencedColumnName = "PK_User_ID")
    private User user;
    
    @Column(name = "SanitizeOption")
    private boolean sanitizeOption;
    
    @Column(name = "AnalyzeFilesOption")
    private boolean analyzeFilesOption;
    
    @Column(name = "NumMailboxes")
    private Integer numMailboxes;
    
    @Column(name = "AnalysisFileSizeQuota")
    private double analysisFileSizeQuota;
    
    @Column(name = "UploadFileSizeQuota")
    private double uploadFileSizeQuota;
    
    @Column(name = "TermStartDate")
    private Date termStartDate;
    
    @Column(name = "TermEndDate")
    private Date termEndDate;
}