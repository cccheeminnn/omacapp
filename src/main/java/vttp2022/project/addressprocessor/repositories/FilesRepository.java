package vttp2022.project.addressprocessor.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import vttp2022.project.addressprocessor.models.File;

import static vttp2022.project.addressprocessor.repositories.Queries.*;

import java.util.LinkedList;
import java.util.List;

@Repository
public class FilesRepository {
    
    @Autowired private JdbcTemplate template;

    public List<File> retrieveFilenames(String email) {
        SqlRowSet rs = template.queryForRowSet(SQL_FILES_RETRIEVE, email);
        
        List<File> fileDetailList = new LinkedList<>();
        while (rs.next()) {
            File myFile = File.populate(rs);
            fileDetailList.add(myFile);
        }

        return fileDetailList;
    }

    //making changes
    public boolean insertFilenames(String email, String queryName, String filename) {
        int count = template.update(SQL_FILES_INSERT, email, queryName, filename);
        return 1 == count;
    }

    public boolean deleteFile(String fileToDelete) {
        int count = template.update(SQL_FILES_DELETE, fileToDelete);
        return 1 == count;
    }
}
