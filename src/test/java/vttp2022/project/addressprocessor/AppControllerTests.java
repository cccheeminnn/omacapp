package vttp2022.project.addressprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import vttp2022.project.addressprocessor.services.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
public class AppControllerTests {

    private static final Logger logger = LoggerFactory.getLogger(AppControllerTests.class);
    
    @Autowired private UserService userSvc;

    @Autowired private MockMvc mvc;

    private String fileName; 

    @Test //username session atttribute exist means login validated
    void shouldReturnIndexView() {
        RequestBuilder reqBuilder = MockMvcRequestBuilders
            .get("/")
            .sessionAttr("username", "notNull");
            //.accept(MediaType.TEXT_HTML_VALUE);

        MvcResult result = null;
        try {
            result = mvc
                .perform(reqBuilder)
                .andReturn();
        } catch (Exception e) {
            fail("failed to perform get req", e);
            return;
        }

        String resultViewName = result.getModelAndView().getViewName();
        logger.info("AppControllerTests shouldReturnIndexView String resultViewName: " + resultViewName);
        assertTrue(resultViewName.equals("index"));
    }

    @Test
    void shouldAskToLogin() {
        RequestBuilder reqBuilder = MockMvcRequestBuilders
            .get("/");

        MvcResult result = null;
        try {
            result = mvc
                .perform(reqBuilder)
                .andReturn();
        } catch (Exception e) {
            fail("failed to perform get req", e);
            return;
        }

        MockHttpServletResponse resp = result.getResponse();
        String redirectedUrl = resp.getRedirectedUrl();
        logger.info("AppControllerTests shouldReturnLoginView String redirectedUrl: " + redirectedUrl);
        assertTrue(redirectedUrl.equals("/login"));
    }

    @Test
    void shouldSuccessfullyUploadAndReturnFileName() {
        Path path = Paths.get("Excel/SG Streets Name - Test.csv");
        String name = "csv-file";
        String originalFileName = "SG Streets Name - Test.csv";
        String contentType = "text/csv";
        byte[] content = null;
        MockMultipartFile mockCsvFile = null;
        try {
            content = Files.readAllBytes(path);
            mockCsvFile = new MockMultipartFile(name, originalFileName, contentType, content);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        byte[] toEmailByte = "omacapp@outlook.com".getBytes();
        RequestBuilder reqBuilder = MockMvcRequestBuilders
            .multipart("/user/upload")
            .file(mockCsvFile)
            .file("toEmail", toEmailByte)
            .sessionAttr("username", "omacapp@outlook.com");

        MvcResult result = null;
        try {
            result = mvc
                .perform(reqBuilder)
                .andReturn();
        } catch (Exception e) {
            fail("failed to perform post(/user/upload) req", e);
            return;
        }
        
        fileName = result.getModelAndView().getModel().get("fileName").toString();
        logger.info("AppControllerTests shouldSuccessfullyUploadAndReturnFileName fileName: " + fileName);
        assertNotNull(fileName);
    }

    @AfterAll
    void deleteTestFile() {
        userSvc.deleteFile(fileName);
    }
}
