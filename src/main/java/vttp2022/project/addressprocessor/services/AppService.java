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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import vttp2022.project.addressprocessor.models.AddressResult;

@Service
public class AppService {
    
    @Autowired 
    private AmazonS3 s3;

    private static final String ONE_MAP_URL = "https://developers.onemap.sg/commonapi/search";

    public Set<String> parseSearchValue(MultipartFile file) throws IOException {

        InputStreamReader isr = new InputStreamReader(file.getInputStream());
        BufferedReader buffReader = new BufferedReader(isr);
        
        //readLine below goes to the first row in the file (column header)
        String rowStr = buffReader.readLine().toLowerCase();
        int addColIndex = getColumnIndex(rowStr, "address");
        System.out.println("Address array index is at " + addColIndex);
        if (addColIndex < 0) {
            System.out.println("\"address\" could not be found in column header");
            return Collections.emptySet();
        }

        Set<String> addressSet = new HashSet<>();
        //iterate rows after col header to add search terms into Address obj
        while ((rowStr = buffReader.readLine()) != null) {
            //.csv separates each column using a , so we split the values by , and find the
            //column index containing the search value
            String[] strArray = rowStr.trim().split(",");
            //why we replace spaces with + is because OneMapAPI does not accept spaces
            String addSearchVal = strArray[addColIndex].replace(" ", "+");
            
            addressSet.add(addSearchVal);
        }
        return addressSet;
    }
    
    public List<AddressResult> queryOneMapAPI(Set<String> addressSet) {
        
        List<AddressResult> addResultList = new ArrayList<>();
        System.out.println("addList(searchVal) length >> " + addressSet.size());
        //API query for each search term
        for (String a : addressSet) {

            String url = UriComponentsBuilder
                .fromUriString(ONE_MAP_URL)
                .queryParam("searchVal", a)
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

                    addResultList.add(addResult);
                });

            } else { //more than 1 page

                //starts at page 1 instead of 0, and <= because we want the last page
                for (int i = 1; i <= totalNumPages; i++) {

                    url = UriComponentsBuilder
                        .fromUriString(ONE_MAP_URL)
                        .queryParam("searchVal", a)
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
            "OMAC/csv/%s.csv".formatted(filename), 
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
    private static int getColumnIndex(String rowStr, String searchStr) {

        System.out.println("row string>" + rowStr);

        int index = -1;
        String[] colHeaderStrArray = rowStr.split(",");
        
        for (int i = 0; i < colHeaderStrArray.length; i++) {
            System.out.println(colHeaderStrArray[i].toLowerCase());
            if (colHeaderStrArray[i].toLowerCase().equals(searchStr)) {
                System.out.println(colHeaderStrArray[i] + " found at index " + i);
                index = i;
            }
        }

        return index;
    }
}