package vttp2022.project.addressprocessor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import vttp2022.project.addressprocessor.services.DigitalOceanService;
import vttp2022.project.addressprocessor.services.UserService;

//test transactional method
@SpringBootTest(classes = AddressprocessorApplication.class)
class MockDOSvcTests {
    
    @Autowired private UserService userSvc;

    @Autowired JdbcTemplate template; 

	@MockBean private DigitalOceanService mockDOSvc;

	@Test
	void shouldRollBackTableDeletion() {
		//deleteFile method invokes 2 method from FilesRepository and DigitalOceanService
		//DigitalOceanService.deleteObjectRequest throws RunTimeException
        String fileToDelete = "c6b500e0"; //already existing file
		Mockito.when(
			mockDOSvc.deleteObjectRequest(fileToDelete))
			.thenThrow(RuntimeException.class);

        try {
            userSvc.deleteFile(fileToDelete); //throws a RuntimeException
        } catch (RuntimeException e) {
            String SQL_FILES_CHECK_IF_FILE_EXIST = "select generatedfilename from files where generatedfilename = ?";
            SqlRowSet rs = template.queryForRowSet(SQL_FILES_CHECK_IF_FILE_EXIST, fileToDelete);
            //this check that the file did not get deleted from our table
            assertTrue(rs.next());
        }
	}

}
