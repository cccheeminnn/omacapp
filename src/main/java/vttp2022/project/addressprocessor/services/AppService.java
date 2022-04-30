package vttp2022.project.addressprocessor.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.opencsv.CSVWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.Address;
import vttp2022.project.addressprocessor.models.AddressResult;

@Service
public class AppService {
    
    @Autowired 
    private AmazonS3 s3;

    private static final String ONE_MAP_URL = "https://developers.onemap.sg/commonapi/search";

    public List<Address> parseSearchValue(MultipartFile file) throws IOException {
        
        InputStreamReader isr = new InputStreamReader(file.getInputStream());
        BufferedReader buffReader = new BufferedReader(isr);
        
        //readLine below goes to the first row in the file
        String addStr = buffReader.readLine().toLowerCase();
        System.out.println("header row String >> " + addStr);
        int addColIndex = getColumnIndex(addStr, "address");
        if (addColIndex < 0) {
            System.out.println("\"address\" could not be found in column header");
            return Collections.emptyList();
        }
        System.out.println("Address array index is at " + addColIndex + " (" + (addColIndex+1) + ")");
        
        List<Address> addList = new ArrayList<>();
        //iterate rows after col header to add search terms into Address obj
        while ((addStr = buffReader.readLine()) != null) {
            String[] strArray = addStr.trim().split(",");
            //Address class is for search values only
            Address myAdd = new Address();
            myAdd.setAddress(strArray[addColIndex].replace(" ", "+"));
            
            addList.add(myAdd);
        }

        isr.close();
        buffReader.close();
        return addList;
    }
    
    public List<AddressResult> queryOneMapAPI(List<Address> addList) {
        
        List<AddressResult> addResultList = new ArrayList<>();
        System.out.println("addList(searchVal) length >> " + addList.size());
        //API query for each search term
        for (Address a : addList) {

            String addressStr = a.getAddress();
            String url = UriComponentsBuilder
                .fromUriString(ONE_MAP_URL)
                .queryParam("searchVal", addressStr)
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

                System.out.println("result only has " + totalNumPages + " page");
                JsonArray array = object.getJsonArray("results");

                array.stream().forEach(v -> {

                    JsonObject obj = v.asJsonObject();

                    AddressResult addResult = AddressResult.create(obj);

                    addResultList.add(addResult);
                    System.out.println(addResult.getFullAddress());
                });

            } else { //more than 1 page

                System.out.println("result has " + totalNumPages + " pages");
                for (int i = 1; i <= totalNumPages; i++) {

                    System.out.println("Page " + i);

                    url = UriComponentsBuilder
                        .fromUriString(ONE_MAP_URL)
                        .queryParam("searchVal", addressStr)
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
                        
                        addResultList.add(addResult);
                        System.out.println(addResult.getFullAddress());
                    });
                }
            }
        }
        return addResultList;
    }

    public String writeToByteArray(List<AddressResult> addResultList) throws WriteToByteArrayException {

        //CSVWriter writes to OutputStreamWriter writes to ByteArrayOutputStream
        //PutObjectRequest InputStream uses ByteArrayOutputStream to write to DO
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try (CSVWriter writer = new CSVWriter(streamWriter)) {

            String[] colHeader = {
                "BLK_NO", 
                "ROAD_NAME", 
                "BUILDING", 
                "ADDRESS", 
                "POSTAL"
            };
            writer.writeNext(colHeader);

            for (AddressResult addResult : addResultList) {

                String[] addressResultObjStr = {
                    addResult.getBlkNo(), 
                    addResult.getRoadName(), 
                    addResult.getBuilding(), 
                    addResult.getFullAddress(), 
                    addResult.getPostalCode()
                };
                writer.writeNext(addressResultObjStr);
            }

            outputStream.close();
            streamWriter.close();
            writer.close();
        } catch (IOException ioe) {
            WriteToByteArrayException wtbae = new WriteToByteArrayException("WriteToByteArrayException occurred");
            throw wtbae; //something went wrong while writing, at this point shouldnt occur 
        } 

        return writeByteArrayToDO(outputStream);
    }

    private String writeByteArrayToDO(ByteArrayOutputStream outputStream) {

        String filename = UUID.randomUUID().toString().substring(0, 8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(outputStream.toByteArray().length);

        PutObjectRequest putReq = new PutObjectRequest(
            "bigcontainer", 
            "OneMapApp/csv/%s.csv".formatted(filename), 
            new ByteArrayInputStream(outputStream.toByteArray()),
            metadata
        );
        putReq.setCannedAcl(CannedAccessControlList.PublicRead);

        try {
            s3.putObject(putReq);
            return filename;
        } catch (SdkClientException sce) {
            return ""; //something went wrong while putting into DO
        }
    }

    //parse the entire col header into a String separated by a comma
    //then into a String array to find the index of "address"
    private static int getColumnIndex(String header, String searchStr){
        String[] colHeaderStrArray = header.trim().split(",");
        return Arrays.asList(colHeaderStrArray).indexOf(searchStr);
    }
}