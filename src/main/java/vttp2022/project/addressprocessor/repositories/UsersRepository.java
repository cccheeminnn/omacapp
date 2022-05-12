package vttp2022.project.addressprocessor.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import static vttp2022.project.addressprocessor.repositories.Queries.*;

@Repository
public class UsersRepository {
    
    @Autowired private JdbcTemplate template;

    public boolean validateLogin(String email, String password) {
        SqlRowSet rs = template.queryForRowSet(SQL_USERS_CHECK_LOGINS, email, password);

        if (rs.next()) 
            return true;
        
        return false;
    }

    public boolean checkUserExist(String email) {
        SqlRowSet rs = template.queryForRowSet(SQL_USERS_CHECK_EXISTS, email);

        if (rs.next())
            return true;
        
        return false;
    }

    //making changes
    public boolean registerUser(String email, String password) {
        int count = template.update(SQL_USERS_REGISTRATION, email, password);

        return 1 == count;
    }
}
