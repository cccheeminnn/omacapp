package vttp2022.project.addressprocessor.services;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.repositories.AddressRepository;
import vttp2022.project.addressprocessor.repositories.FilesRepository;

//contains all the business logic
@Service
public class AppService {

    @Autowired private AddressRepository omacRepo;

    @Autowired private DigitalOceanService doSvc;

    @Autowired private EmailService emailSvc;
    
    @Autowired private FilesRepository filesRepo;

    private static final String ONE_MAP_URL = "https://developers.onemap.sg/commonapi/search";
    
    //for .csv file upload
    //used when uploaded csv file has < 20 searches
    public String queryOneMapAPI(Set<String> addressSet) 
    {
        List<AddressResult> addResultList = new ArrayList<>();
        System.out.println("No of queries (<20) >> " + addressSet.size());
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

        try {
            String fileName = doSvc.writeToByteArray(addResultList);
            return fileName;
        } catch (WriteToByteArrayException e) {
            return ""; //returns empty String if exception occurs
        }
    }

    //used when uploded .csv file has >= 20 searches
    @Async("threadPoolTaskExecutor")
    public void queryOneMapAPIAsync(Set<String> addressSet, String toEmail, String queryName) 
    {
        List<AddressResult> addResultList = new ArrayList<>();
        System.out.println("addList(searchVal) length >> " + addressSet.size());

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

            if (object.getInt("found") == 0) {
                continue;
            }

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
            } else {
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

        //once the query completes and all results are parse into a List<AddressResult>
        try {
            String filename = doSvc.writeToByteArray(addResultList);
            emailSvc.sendEmailWithAttachment(toEmail, filename);
            filesRepo.insertFilenames(toEmail, queryName, filename);
        } catch (WriteToByteArrayException e) {
            e.printStackTrace(); //@Async method failed to write to do spaces
        } catch (MessagingException e) {
            e.printStackTrace(); //@Async method failed to send email out
        } catch (MalformedURLException murle) {
            murle.printStackTrace(); //@Async method URL to file gone wrong
        }
    }
    //end

    //for quick search
    public List<AddressResult> getAddressesFromSearchValue (
        String searchTerm, Integer limit, Integer offset, String searchBy) 
    {
        List<AddressResult> addResultsList = new LinkedList<>();
        addResultsList = omacRepo.getFullAddresses(searchTerm, limit, offset, searchBy);

        return addResultsList;
    }

    public Integer getNumberOfResults(String searchTerm, String searchBy) 
    {
        return omacRepo.getNumberOfResults(searchTerm, searchBy);
    }
    //end

}