package vttp2022.project.addressprocessor.repositories;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import vttp2022.project.addressprocessor.models.AddressResult;

import static vttp2022.project.addressprocessor.repositories.Queries.*;

import java.util.LinkedList;
import java.util.List;

@Repository
public class OMACRepository {
    
    @Autowired private JdbcTemplate template;

    public Integer getNumberOfResults(String searchTerm, String searchBy) {

        SqlRowSet rs = null;
        switch (searchBy) {
            case "address":
                rs = template.queryForRowSet(SQL_NUMBER_OF_RESULTS_BY_FULL_ADDRESS, searchTerm);
                break;
            case "postalcode":
                rs = template.queryForRowSet(SQL_NUMBER_OF_RESULTS_BY_POSTAL_CODE, searchTerm);
                break;
        }

        Integer noOfResults = 0;

        if (rs.next()) 
            noOfResults = rs.getInt("count");
         
        return noOfResults;

    }

    public List<AddressResult> getFullAddresses(String searchTerm, Integer limit, Integer offset, String searchBy) {

        SqlRowSet rs = null;
        switch (searchBy) {
            case "address":
                System.out.println("CASE: ADDRESS");
                rs = template.queryForRowSet(SQL_FIND_ADDRESSES_BY_FULL_ADDRESS, searchTerm, limit, offset);
                break;
            case "postalcode":
                System.out.println("CASE: POSTAL");
                rs = template.queryForRowSet(SQL_FIND_ADDRESSES_BY_POSTAL_CODE, searchTerm, limit, offset);
                break;
        }

        List<AddressResult> addResultsList = new LinkedList<>();
        while (rs.next()) {
            AddressResult myAddResult = AddressResult.populate(rs);
            addResultsList.add(myAddResult);
        }

        return addResultsList;
    }
}