package com.nteligen.hq.dhs.siaft.processors;

import static org.junit.Assert.assertEquals;

import com.nteligen.hq.dhs.siaft.dal.FileAttributeDAL;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;

import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import org.easymock.EasyMock;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs tests of the FileAttributesWriter Processor. The primary goal of
 * these tests is to exercise the onTrigger() method by means of the NiFi
 * TestRunner utility. Calls to the Data Access Layer (DAL) are mocked to avoid
 * the need for an active database connection during testing.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(FileAttributeWriter.class)
public class FileAttributeWriterTest
{
  private static final String DATA = "foo";

  /**
   * Tests that the processor does not make any changes to the flow file
   * content. Also verifies that calls to the Data Access Layer via
   * FileAttributeDAL.createNewFileAttribute() contain the proper parameters.
   */
  @org.junit.Test()
  public void testFileAttributeWriter() throws Exception
  {
    String uuid = "0c7a43ef-20f0-4d77-a783-e9085d902391";
    String fileName = "foo";
    long submissionId = 0;
    String mimeType = "pdf";
    String md5 = "acbd18db4cc2f85cedef654fccc4a4d8";  // md5 of "foo"
    String sha256 = "someSHA256";

    FileAttribute attribute = new FileAttribute();
    attribute.setFileName(fileName);
    attribute.setFileType(mimeType);
    attribute.setOriginalUuid(uuid);
    attribute.setMd5(md5);
    attribute.setFileAttributeId(7);

    final TestRunner runner = TestRunners.newTestRunner(new FileAttributeWriter());

    // setup mock to intercept FileAttributeDAL creation
    FileAttributeDAL mock = PowerMock.createMock(FileAttributeDAL.class);
    PowerMock.expectNew(FileAttributeDAL.class).andReturn(mock);
    EasyMock.expect(mock.createNewFileAttribute(fileName, submissionId, mimeType, md5, sha256, uuid))
        .andReturn(attribute);
    PowerMock.replayAll();

    // set flowfile attributes for test
    Map<String,String> attributes = new HashMap<String,String>();
    attributes.put(CoreAttributes.FILENAME.key(), "foo");
    attributes.put(SIAFTUtils.MIME_TYPE, "pdf");
    attributes.put(CoreAttributes.UUID.key(), "0c7a43ef-20f0-4d77-a783-e9085d902391");

    InputStream content = new ByteArrayInputStream(DATA.getBytes("UTF-8"));

    runner.enqueue(content, attributes);
    runner.run(1);
    runner.assertQueueEmpty();

    List<MockFlowFile> success = runner.getFlowFilesForRelationship(
        FileAttributeWriter.SUCCESS_BEHAVIOR.successRelationship);
    MockFlowFile successFlowFile = success.get(0);

    // ensure no changes to the file content
    assertEquals(DATA, new String(runner.getContentAsByteArray(successFlowFile)));

    // verify that the file attributes id attribute is set
    assertEquals("7", successFlowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
    PowerMock.verifyAll();
  }

}
