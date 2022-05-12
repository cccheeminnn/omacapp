package vttp2022.project.addressprocessor.models;

import org.springframework.jdbc.support.rowset.SqlRowSet;

public class File {
    
    private String email;
    private String queryName;
    private String generatedFileName;
    
    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}

    public String getQueryName() {return queryName;}
    public void setQueryName(String queryName) {this.queryName = queryName;}

    public String getGeneratedFileName() {return generatedFileName;}
    public void setGeneratedFileName(String generatedFileName) {this.generatedFileName = generatedFileName;}

    public static File populate(SqlRowSet rs) {
        File myFile = new File();
        myFile.setEmail(rs.getString("email"));
        myFile.setQueryName(rs.getString("queryname"));
        myFile.setGeneratedFileName(rs.getString("generatedfilename"));
        return myFile;
    }
    
}