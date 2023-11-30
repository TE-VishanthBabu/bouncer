package com.nteligen.hq.dhs.siaft.processors;

import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.lang.NullPointerException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


@SideEffectFree
@Tags({"Null Pointer", "Test"})
@CapabilityDescription("Throws a Null Pointer Exception if filename contains capital letter 'A'")
public class NullPointerProcessor extends AbstractProcessor
{

  private List<PropertyDescriptor> properties;
  private Set<Relationship> relationships;

  public static final Relationship SUCCESS = new Relationship.Builder()
      .name("successRelationship")
      .description("Success relationship")
      .build();

  @Override
  public void init(final ProcessorInitializationContext context)
  {
    List<PropertyDescriptor> properties = new ArrayList<>();
    this.properties = Collections.unmodifiableList(properties);

    Set<Relationship> relationships = new HashSet<>();
    relationships.add(SUCCESS);
    this.relationships = Collections.unmodifiableSet(relationships);
  }

  @Override
  public void onTrigger(final ProcessContext context,
                        final ProcessSession session)
    throws NullPointerException, ProcessException
  {

    final AtomicReference<String> value = new AtomicReference<>();

    getLogger().trace("Processing session " + session);
    FlowFile flowfile = session.get();
    getLogger().trace("Processing flowfile " + flowfile);

    String filename = flowfile.getAttribute("filename");
    getLogger().info("Filename: " + filename);

    if (filename.contains("A"))
    {
      throw new NullPointerException("Test Exception");
    }

    session.transfer(flowfile, SUCCESS);
  }

  @Override
  public Set<Relationship> getRelationships()
  {
    return relationships;
  }

}
