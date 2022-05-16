package vttp2022.project.addressprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;

import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.services.DigitalOceanService;
import vttp2022.project.addressprocessor.services.EmailService;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
public class DigitalOceanServiceTests {
	
	Logger logger = LoggerFactory.getLogger(DigitalOceanServiceTests.class);

	@Autowired private DigitalOceanService doSvc;

	@Autowired private EmailService emailSvc;
	
	private String filename;

	@BeforeAll
	void createTestFile() {
		List<AddressResult> addResultList = new LinkedList<>();
		AddressResult myAddResult = new AddressResult();
		myAddResult.setBlkNo("test blkno");
		myAddResult.setRoadName("test roadname");	
		myAddResult.setBuilding("test building");
		myAddResult.setFullAddress("test fulladdress");
		myAddResult.setPostalCode("test postalcode");
		addResultList.add(myAddResult);
		try {
			filename = doSvc.writeToByteArray(addResultList);
			logger.info("DOSvcTests.java createTestFile filename is: " + filename);
		} catch (WriteToByteArrayException wtbae) {System.out.println("error in createRestFiles");}
	}	

	@Test
	void shouldThrowMailSendException() {
		assertThrows(MailSendException.class, () -> {
			emailSvc.sendEmailWithAttachment("omacapp@outlook.com", "doesNotExist");
		});	
	}	

	@Test
	void shouldSendEmailSuccessfully() {
		boolean emailSentSuccessfully = false;	
		try {
			//test.csv file created manually	
			emailSvc.sendEmailWithAttachment("omacapp@outlook.com", filename);
			emailSentSuccessfully = true;
		} catch (Exception e) {}
		assertTrue(emailSentSuccessfully); //manually check email received
	}
	
	@AfterAll
	void deleteTestFile() {
		doSvc.deleteObjectRequest(filename);
	}
}
