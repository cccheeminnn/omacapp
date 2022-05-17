package vttp2022.project.addressprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.services.AppService;
import vttp2022.project.addressprocessor.services.DigitalOceanService;
import vttp2022.project.addressprocessor.services.UserService;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class AppServiceTests {

    private static final Logger logger = LoggerFactory.getLogger(AppServiceTests.class);
    
    @Autowired private AppService appSvc;

    @Autowired private DigitalOceanService doSvc;

    @Autowired private UserService userSvc;

    private Set<String> addressQueryString = new HashSet<>();

    private String filename;

    private String asyncFilename;

    @BeforeAll //create search queries for API call
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

    @Test
    void shouldReturnAFileNameAsync() {
        Future<String> future = appSvc.queryOneMapAPIAsync(addressQueryString, "omacapp@outlook.com", "shouldReturnAFileNameAsyncTest");
        try {
            asyncFilename = future.get();
        } catch (Exception e) {}
        logger.info("AppServiceTests shouldReturnAFileNameAsync asyncFilename is : " + asyncFilename);
        assertFalse(asyncFilename.isEmpty());
    }

    @Test //quickSearch test
    void return12SearchTermResults() {
        String searchTerm = "%loyang way 2%"; //12 results
        List<AddressResult> list = appSvc.getAddressesFromSearchValue(searchTerm, 10000, 0, "address");
        int noOfResults = appSvc.getNumberOfResults(searchTerm, "address");
        assertEquals(12, list.size());
        assertEquals(12, noOfResults);
    }

    @Test //quickSearch test
    void shouldReturnNoResults() {
        String searchTerm = "%loyang way 2%"; //0 results searched under postalcode
        List<AddressResult> list = appSvc.getAddressesFromSearchValue(searchTerm, 10000, 0, "postalcode");
        int noOfResults = appSvc.getNumberOfResults(searchTerm, "postalcode");
        assertEquals(0, list.size());
        assertEquals(0, noOfResults);

        list = appSvc.getAddressesFromSearchValue(searchTerm, 10000, 0, "building");
        noOfResults = appSvc.getNumberOfResults(searchTerm, "building");
        assertEquals(0, list.size());
        assertEquals(0, noOfResults);
    }
    
	@AfterAll //delete the files restore state
	void deleteTestFile() {
		doSvc.deleteObjectRequest(filename);
        userSvc.deleteFile(asyncFilename);
	}
}
