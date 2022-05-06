package vttp2022.project.addressprocessor.services;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import vttp2022.project.addressprocessor.models.AddressResult;
import static vttp2022.project.addressprocessor.repositories.Queries.*;


//parse in the file from Excel folder and refresh the database
//excel file consist of all the streets name in singapore 
//probably dont have to do this often

public class UpdateAddressDatabase {

    private static final String ONE_MAP_URL = "https://developers.onemap.sg/commonapi/search";
    //just run
    public static void main(String[] args) {

        String path = "Excel/SG Streets Name.csv";

        try {
            BufferedReader buffReader = new BufferedReader(new FileReader(path));
            String line;
            Set<String> stringSet = new HashSet<>();
            //skip the column header
            buffReader.readLine();
            while ((line = buffReader.readLine()) != null) {
                stringSet.add(line);
            }
            buffReader.close();

            List<AddressResult> addResultList = new LinkedList<>();
            for (String a : stringSet) {
                
                String url = UriComponentsBuilder
                    .fromUriString(ONE_MAP_URL)
                    .queryParam("searchVal", a.replace(" ", "+"))
                    .queryParam("returnGeom", "N")
                    .queryParam("getAddrDetails", "Y")
                    .queryParam("pageNum", "1")
                    .toUriString();

                RequestEntity<Void> req = RequestEntity.get(url).build();                    
                RestTemplate template = new RestTemplate();
                ResponseEntity<String> resp = template.exchange(req, String.class);
                JsonReader reader = Json.createReader(new StringReader(resp.getBody()));
                JsonObject object = reader.readObject();
    
                //if the search value returns 0 results, we skip to the next one
                if (object.getInt("found") == 0) {
                    continue;
                }
    
                //some results has 1 page, some more than 1
                int totalNumPages = object.getInt("totalNumPages");
                if (totalNumPages == 1) {
    
                    JsonArray array = object.getJsonArray("results");
    
                    array.stream().forEach(v -> {
    
                        JsonObject obj = v.asJsonObject();
    
                        AddressResult addResult = AddressResult.create(obj);
    
                        if (!addResult.getPostalCode().equals("NIL")) {
                            addResultList.add(addResult);
                        } 
                        
                    });
    
                } else { //more than 1 page
    
                    //starts at page 1 instead of 0 and <= because we want the last page
                    for (int i = 1; i <= totalNumPages; i++) {
    
                        url = UriComponentsBuilder
                            .fromUriString(ONE_MAP_URL)
                            .queryParam("searchVal", a.replace(" ", "+"))
                            .queryParam("returnGeom", "N")
                            .queryParam("getAddrDetails", "Y")
                            .queryParam("pageNum", i)
                            .toUriString();
    
                        req = RequestEntity.get(url).build();
                        resp = template.exchange(req, String.class);
                        reader = Json.createReader(new StringReader(resp.getBody()));
                        object = reader.readObject();
                        JsonArray array = object.getJsonArray("results");
    
                        array.stream().forEach(v -> {
    
                            JsonObject obj = v.asJsonObject();
    
                            AddressResult addResult = AddressResult.create(obj);
                            
                            if (!addResult.getPostalCode().equals("NIL")) {
                                addResultList.add(addResult);
                            }
                        });
                    }
                }
            }

            JdbcTemplate template = new JdbcTemplate();
            // DataSourceBuilder<Void> builder = DataSourceBuilder.create().url("url");

            template.setDataSource(
                DataSourceBuilder.create()
                    .url(System.getenv("SPRING_DATASOURCE_URL"))
                    .username(System.getenv("SPRING_DATASOURCE_USERNAME"))
                    .password(System.getenv("SPRING_DATASOURCE_PASSWORD"))
                    .build());
            
            template.update(SQL_TRUNCATE_TABLE_ADDRESSES);
            
            for (AddressResult obj : addResultList) {
                //(BLK_NO, ROAD_NAME, BUILDING, FULL_ADDRESS, POSTAL_CODE)
                template.update(SQL_INSERT_ADDRESS, 
                    obj.getBlkNo(), 
                    obj.getRoadName(), 
                    obj.getBuilding(), 
                    obj.getFullAddress(), 
                    obj.getPostalCode());
            }

        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}