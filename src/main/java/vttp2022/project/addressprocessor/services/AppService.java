package vttp2022.project.addressprocessor.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
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

import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.repositories.OMACRepository;

@Service
public class AppService {

    @Autowired private OMACRepository omacRepo;
    
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

        //use a set to avoid duplicate value search
        Set<String> addressSet = new HashSet<>();
        //iterate rows after col header to add search terms into addressSet
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

                    if (!addResult.getPostalCode().equals("NIL")) {
                        addResultList.add(addResult);
                    } 
                    
                });

            } else { //more than 1 page

                //starts at page 1 instead of 0 and <= because we want the last page
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
                        
                        if (!addResult.getPostalCode().equals("NIL")) {
                            addResultList.add(addResult);
                        }
                    });
                }
            }
        }
        return addResultList;
    }

    //parse the entire col header into a String separated by a comma
    //then into a String array to find the index of "address"
    private static int getColumnIndex(String rowStr, String searchStr) {
        System.out.println("rowStr >>> " + rowStr);
        String[] colHeaderStrArray = rowStr.split(",");
        return ArrayUtils.indexOf(colHeaderStrArray, searchStr);
    }

    //for index page search fields result
    public List<AddressResult> getAddressesFromSearchValue (String searchTerm, Integer limit, Integer offset) {

        List<AddressResult> addResultsList = new LinkedList<>();

        addResultsList = omacRepo.getFullAddresses(searchTerm, limit, offset);

        return addResultsList;
    }

    public Integer getNumberOfResults(String searchTerm) {
        return omacRepo.getNumberOfResults(searchTerm);
    }
}