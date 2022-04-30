package vttp2022.project.addressprocessor.models;

//initially used to hold search values from .csv file, but it doesnt help to
//remove duplicate search values. update to use a Set<String> instead

public class Address {
    
    private String address;

    public String getAddress() {return address;}
    public void setAddress(String address) {this.address = address;}
   
}