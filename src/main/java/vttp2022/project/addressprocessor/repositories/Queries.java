package vttp2022.project.addressprocessor.repositories;

public interface Queries {

    //mainly for use in the app

    //users table
    public static final String SQL_USERS_CHECK_LOGINS = "select * from users where email = ? and password = sha1(?)";

    public static final String SQL_USERS_CHECK_EXISTS = "select email from users where email = ?";

    public static final String SQL_USERS_REGISTRATION = "insert into users (email, password) values (?, sha1(?))";
    //users table end

    //files table
    public static final String SQL_FILES_INSERT = "insert into files (email, queryname, generatedfilename) values (?, ?, ?)";

    public static final String SQL_FILES_RETRIEVE = "select * from files where email = ?";

    public static final String SQL_FILES_DELETE = "delete from files where generatedfilename = ?";
    //files table end

    //address table
    public static final String SQL_NUMBER_OF_RESULTS_BY_FULL_ADDRESS = 
        """
            select count(*) as count 
                from addresses where full_address like ?
        """;

    public static final String SQL_NUMBER_OF_RESULTS_BY_POSTAL_CODE = 
        """
            select count(*) as count 
                from addresses where postal_code like ?
        """;

    public static final String SQL_NUMBER_OF_RESULTS_BY_BUILDING = 
        """
            select count(*) as count 
                from addresses where building like ?
        """;


    public static final String SQL_FIND_ADDRESSES_BY_FULL_ADDRESS = "select * from addresses where full_address like ? limit ? offset ?";

    public static final String SQL_FIND_ADDRESSES_BY_POSTAL_CODE = "select * from addresses where postal_code like ? limit ? offset ?";

    public static final String SQL_FIND_ADDRESSES_BY_BUILDING = "select * from addresses where building like ? limit ? offset ?";
    //address table end

    //end

    //mainly for use in UpdateAddressDatabase.java

    public static final String SQL_INSERT_ADDRESS = 
        """
            insert into addresses (BLK_NO, ROAD_NAME, BUILDING, FULL_ADDRESS, POSTAL_CODE)
                values (?, ?, ?, ?, ?)
        """;

    public static final String SQL_TRUNCATE_TABLE_ADDRESSES = "truncate table addresses";

    //end
}
