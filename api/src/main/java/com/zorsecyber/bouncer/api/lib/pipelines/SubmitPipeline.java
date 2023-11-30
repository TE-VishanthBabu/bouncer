package com.zorsecyber.bouncer.api.lib.pipelines;

import java.io.File;
import java.util.Map;

import com.zorsecyber.bouncer.api.exceptions.PipelineException;

public interface SubmitPipeline {
	public Map<String, Object> run(File file) throws PipelineException;
}
