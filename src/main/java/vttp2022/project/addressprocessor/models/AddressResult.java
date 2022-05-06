package vttp2022.project.addressprocessor.models;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import jakarta.json.JsonObject;

public class AddressResult {
    private String blkNo;
    private String roadName;
    private String building;
    private String fullAddress;
    private String postalCode;

    public String getBlkNo() {return blkNo;}
    public void setBlkNo(String blkNo) {this.blkNo = blkNo;}
    
    public String getRoadName() {return roadName;}
    public void setRoadName(String roadName) {this.roadName = roadName;}
    
    public String getBuilding() {return building;}
    public void setBuilding(String building) {this.building = building;}
    
    public String getFullAddress() {return fullAddress;}
    public void setFullAddress(String fullAddress) {this.fullAddress = fullAddress;}
    
    public String getPostalCode() {return postalCode;}
    public void setPostalCode(String postalCode) {this.postalCode = postalCode;}

    //from API
    public static AddressResult create(JsonObject object) {
        AddressResult addResult = new AddressResult();
        addResult.setBlkNo(object.getString("BLK_NO"));
        addResult.setRoadName(object.getString("ROAD_NAME"));
        addResult.setBuilding(object.getString("BUILDING"));
        addResult.setFullAddress(object.getString("ADDRESS"));
        addResult.setPostalCode(object.getString("POSTAL"));
        return addResult;
    }

    //from database
    public static AddressResult populate(SqlRowSet rs) {
        AddressResult addResult = new AddressResult();
        addResult.setBlkNo(rs.getString("BLK_NO"));
        addResult.setRoadName(rs.getString("ROAD_NAME"));
        addResult.setBuilding(rs.getString("BUILDING"));
        addResult.setFullAddress(rs.getString("FULL_ADDRESS"));
        addResult.setPostalCode(Integer.toString(rs.getInt("POSTAL_CODE")));
        return addResult;
    }
}