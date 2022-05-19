package vttp2022.project.addressprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import vttp2022.project.addressprocessor.exceptions.UserAlreadyExistException;
import vttp2022.project.addressprocessor.repositories.UsersRepository;
import vttp2022.project.addressprocessor.services.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class AppControllerTests {

    private static final Logger logger = LoggerFactory.getLogger(AppControllerTests.class);
    
    @Autowired private UserService userSvc;

    @Autowired private MockMvc mvc;

    @Autowired private JdbcTemplate template;

    @Autowired private UsersRepository usersRepo;

    private String fileNameUploadBelow20; 

    private String fileNameQuickSearch; 

    private String createEmail = "createEmailTest@test.com";
    private String createEmailPw = "createEmailPw";

    @Test //username session atttribute exist means login validated
    @Order(1)
    void shouldReturnIndexView() {
        RequestBuilder reqBuilder = MockMvcRequestBuilders
            .get("/")
            .sessionAttr("username", "notNull");

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
    @Order(2)
    void shouldAskToLogin() {
        RequestBuilder reqBuilder = MockMvcRequestBuilders
            .get("/user/profile"); // user/* protected

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
    @Order(3)
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
        
        fileNameUploadBelow20 = result.getModelAndView().getModel().get("fileName").toString();
        logger.info("AppControllerTests shouldSuccessfullyUploadAndReturnFileName fileName: " + fileNameUploadBelow20);
        assertNotNull(fileNameUploadBelow20);
    }

    @Test
    @Order(4)
    void shouldReturnAFileNameForQuickSearchDownload() {
        try {
            RequestBuilder reqBuilder = MockMvcRequestBuilders
                .post("/user/downloadsearchresults")
                .sessionAttr("username", "omacapp@outlook.com")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("searchBy", "address")
                .param("searchValue", "loyang way 2");

            MvcResult result = mvc
                .perform(reqBuilder)
                .andExpect(status().isOk())
                .andReturn();

            fileNameQuickSearch = result.getModelAndView().getModel().get("fileName").toString();
            logger.info("AppControllerTests shouldReturnAFileNameForQuickSearchDownload fileNameQuickSearch:" + fileNameQuickSearch);
            assertNotNull(fileNameQuickSearch);
            assertTrue(result.getModelAndView().getViewName().equals("download")); //go to download page
        } catch (Exception e) {
            fail("failed test shouldReturnAFileNameForQuickSearchDownload", e);
            return;
        }
    }

    @Test
    @Order(5) //run after shouldReturnAFileNameForQuickSearchDownload
    void shouldDeleteFileCreated() {
        try {
            RequestBuilder reqBuilder = MockMvcRequestBuilders
                .post("/user/profile/delete")
                .sessionAttr("username", "omacapp@outlook.com")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("fileToDelete", fileNameQuickSearch);

            MvcResult result = mvc
                .perform(reqBuilder)
                .andExpect(status().is3xxRedirection())
                .andReturn();

            MockHttpServletResponse resp = result.getResponse();
            assertEquals(302, resp.getStatus());

            String SQL_FILES_CHECK_IF_FILE_EXIST = "select generatedfilename from files where generatedfilename = ?";
            SqlRowSet rs = template.queryForRowSet(SQL_FILES_CHECK_IF_FILE_EXIST, fileNameQuickSearch);
            //this check that the file is deleted from our table
            assertFalse(rs.next());
        } catch (Exception e) {
            fail("failed test shouldDeleteFileCreated", e);
            return;
        }
    }

    @Test
    @Order(6)
    void shouldLoginAndRedirect() {
        try {
            RequestBuilder reqBuilder = MockMvcRequestBuilders
                .post("/login")
                .sessionAttr("username", "omacapp@outlook.com")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("loginEmail", "omacapp@outlook.com")
                .param("loginPassword", "12345");
            
            MvcResult result = mvc
                .perform(reqBuilder)
                .andExpect(status().is3xxRedirection())
                .andReturn();
        
            MockHttpServletResponse resp = result.getResponse();
            assertEquals(302, resp.getStatus());
        } catch (Exception e) {
            fail("failed test shouldLoginAndRedirect", e);
            return;
        }
    }

    @Test
    @Order(7)
    void shouldRegisterAnAccount() {
        try {
            RequestBuilder reqBuilder = MockMvcRequestBuilders
                .post("/register")
                .sessionAttr("username", "omacapp@outlook.com")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("loginEmail", createEmail)
                .param("loginPassword", createEmailPw);

            MvcResult result = mvc
                .perform(reqBuilder)
                .andReturn();

            assertTrue(usersRepo.checkUserExist(createEmail));
        } catch (Exception e) {
            fail("failed test shouldRegisterAnAccount", e);
            return;
        }
    }

    @Test
    @Order(8)
    void shouldNotRegisterTheSameAccount() {
        try {
            RequestBuilder reqBuilder = MockMvcRequestBuilders
                .post("/register")
                .sessionAttr("username", "omacapp@outlook.com")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("loginEmail", createEmail)
                .param("loginPassword", createEmailPw);

            MvcResult result = mvc
                .perform(reqBuilder)
                .andExpect(status().isOk())
                .andReturn();

        } catch (UserAlreadyExistException uaee) {
            assertTrue(true);
        } catch (Exception e) {
            fail("failed test shouldNotRegisterTheSameAccount", e);
            return;
        } 
    }

    @Test
    @Order(9)
    void shouldReturnOkAndIndex() {
        try {
            RequestBuilder reqBuilder = MockMvcRequestBuilders
                .get("/user/quicksearch")
                .queryParam("searchValue", "loyang way 2") //12 results
                .queryParam("searchBy", "address")
                .queryParam("page", "1")
                .sessionAttr("username", "omacapp@outlook.com");
    
            MvcResult result = mvc
                .perform(reqBuilder)
                .andReturn();
            
            String noOfResults = result.getModelAndView().getModel().get("noOfResults").toString();
            String viewname = result.getModelAndView().getViewName();
            assertTrue(noOfResults.equals("12"));
            assertTrue(viewname.equals("index"));

        } catch (Exception e) {
            fail("failed test shouldReturnOkAndIndex", e);
            return;
        }
    }

    @AfterAll
    void deleteTestFile() {
        //delete the file created from test
        userSvc.deleteFile(fileNameUploadBelow20);

        //delete the user created from test
        String SQL_USERS_DELETE = "delete from users where email = ?";
        template.update(SQL_USERS_DELETE, createEmail);
    }
}
