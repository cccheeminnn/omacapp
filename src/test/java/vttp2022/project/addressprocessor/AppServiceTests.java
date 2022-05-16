package vttp2022.project.addressprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import vttp2022.project.addressprocessor.services.AppService;
import vttp2022.project.addressprocessor.services.DigitalOceanService;

@SpringBootTest(classes = AddressprocessorApplication.class)
@TestInstance(Lifecycle.PER_CLASS)
public class AppServiceTests {

    Logger logger = LoggerFactory.getLogger(AppServiceTests.class);
    
    @Autowired private AppService appSvc;

    @Autowired private DigitalOceanService doSvc;

    private Set<String> addressQueryString = new HashSet<>();

    private String filename;

    @BeforeAll
    void createSearchQuery() {
        addressQueryString.add("Changi+Village+Road");
        addressQueryString.add("Dawson+Road");
        addressQueryString.add("Cambridge+Road");
        addressQueryString.add("ajksdfhajkdfhkasj"); //found = 0
    }

    @Test
    void shouldReturnAFileName () {
        filename = appSvc.queryOneMapAPI(addressQueryString);
        logger.info("AppServiceTests shouldReturnAFileName filename is : " + filename);
        assertFalse(filename.isEmpty());
    }

	@AfterAll
	void deleteTestFile() {
		doSvc.deleteObjectRequest(filename);
	}
}
