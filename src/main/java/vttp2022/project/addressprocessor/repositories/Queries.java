package vttp2022.project.addressprocessor.repositories;

public interface Queries {

    //mainly for use in the app
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

    public static final String SQL_FIND_ADDRESSES_BY_FULL_ADDRESS = "select * from addresses where full_address like ? limit ? offset ?";

    public static final String SQL_FIND_ADDRESSES_BY_POSTAL_CODE = "select * from addresses where postal_code like ? limit ? offset ?";
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
