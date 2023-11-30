package com.nteligen.hq.dhs.siaft.processors;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AnalysisEngineBehavior implements SIAFTBehaviorRetrievable
{

  public final PropertyDescriptor analysisEngineName;

  /**
   * Constructor.
   *
   * @param engineName The default value for the analysis engine name property.
   */
  public AnalysisEngineBehavior(String engineName)
  {
    analysisEngineName = new PropertyDescriptor.Builder()
        .name(SIAFTAnalysisProcessor.ANALYSIS_ENGINE_NAME_PROPERTY)
        .description("The name of the Analysis Engine. This must match the "
                     + "EngineName of an engine in the AnalyzeEngine table in "
                     + "the database.")
        .required(true).defaultValue(engineName)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();
  }

  @Override
  public List<PropertyDescriptor> getProperties()
  {
    List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(analysisEngineName);

    return properties;
  }

  @Override
  public Set<Relationship> getRelationships()
  {
    return Collections.emptySet();
  }

}
