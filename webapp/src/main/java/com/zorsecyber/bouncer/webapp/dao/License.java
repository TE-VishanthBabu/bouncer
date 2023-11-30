package com.zorsecyber.bouncer.webapp.dao;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
    
    @Column(name = "UserEmail")
    private String userEmail;
    
    @OneToOne
    @JoinColumn(name = "Organization")
	private Organizations organization;
    
    @Column(name = "SanitizeOption")
    private Boolean sanitizeOption;
    
    @Column(name = "AnalyzeFilesOption")
    private Boolean analyzeFilesOption;
    
    @Column(name = "NumMailboxes")
    private Integer numMailboxes;
    
    @Column(name = "AnalysisFileSizeQuota")
    private Double analysisFileSizeQuota;
    
    @Column(name = "UploadFileSizeQuota")
    private Double uploadFileSizeQuota;
    
    @Column(name = "TermStartDate")
    private Date termStartDate;
    
    @Column(name = "TermEndDate")
    private Date termEndDate;
}
