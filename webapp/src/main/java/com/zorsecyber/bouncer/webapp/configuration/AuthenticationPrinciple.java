package com.zorsecyber.bouncer.webapp.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuthenticationPrinciple implements Serializable {
	private static final long serialVersionUID = -7766083844514854293L;
	
	private String id;
    private String email;
    private List<String> authority;
}
