package vttp2022.project.addressprocessor.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.services.AppService;
import vttp2022.project.addressprocessor.services.DigitalOceanService;

@Controller
@RequestMapping(path="")
public class AppController {

    @Autowired private AppService appSvc;
    
    @Autowired private DigitalOceanService doSvc;

    //Get homepage
    @GetMapping(path={"", "/upload"})
    public ModelAndView getIndex() {       
        ModelAndView mvc = new ModelAndView();
        mvc.addObject("message", "");
        mvc.addObject("fileName", "");

        mvc.addObject("resultList", Collections.emptyList());
        mvc.addObject("searchValue", "");
        mvc.addObject("noOfResults", "");
        mvc.addObject("page", 1);
        mvc.setViewName("index");
        return mvc;
    }

    @PostMapping(path="/upload")
    public ModelAndView postUpload(@RequestParam("csv-file") MultipartFile file) {
        
        ModelAndView mvc = new ModelAndView();

        //application/vnd.ms-excel - .csv
        System.out.println("uploaded file contentType is " + file.getContentType());
        //if the file is empty or if the file uploaded is not .csv format
        if(file.isEmpty() || !file.getContentType().equals("application/vnd.ms-excel")) {
            
            mvc.addObject("message", "invalidfiletype");
            mvc.setViewName("error");
            return mvc;

        } else {

            try {

                Set<String> searchValSet = appSvc.parseSearchValue(file);
                if (searchValSet.isEmpty()) {
                    mvc.addObject("message", "addressnotincolheader");
                    mvc.setViewName("error");
                    return mvc;    
                }

                List<AddressResult> queryResultList = appSvc.queryOneMapAPI(searchValSet);

                String filename = doSvc.writeToByteArray(queryResultList);
                if (filename.isBlank()) {
                    mvc.addObject("message", "douploadwentwrong");
                    mvc.setViewName("error");
                    return mvc;    
                }
                
                mvc.addObject("message", "Here is your search results!");
                mvc.addObject("fileName", filename);

                mvc.addObject("resultList", Collections.emptyList());
                mvc.addObject("searchValue", "");
                mvc.addObject("noOfResults", "");
                mvc.addObject("page", 1);
                mvc.setViewName("index");
                return mvc;        

            } catch (IOException ioe) {
                mvc.addObject("message", "ioe");
                mvc.setViewName("error");
                return mvc;
            } catch (WriteToByteArrayException wtbae) {
                mvc.addObject("message", "wtbae");
                mvc.setViewName("error");
                return mvc;
            }
        }
    }

    //after user typed in search term and hit search
    @PostMapping(path="/quicksearch")
    public ModelAndView postQuickSearch(@RequestBody MultiValueMap<String, String> formData) {

        ModelAndView mvc = new ModelAndView();

        String searchTerm = formData.getFirst("searchValue");
        String searchTermForSQL = "%" + searchTerm + "%";
        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 10, 0);

        Integer noOfResults = appSvc.getNumberOfResults(searchTermForSQL);

        mvc.addObject("message", "");
        mvc.addObject("fileName", "");
        
        mvc.addObject("resultList", addResultsList);
        mvc.addObject("searchValue", searchTerm);
        mvc.addObject("noOfResults", noOfResults);
        mvc.addObject("page", 1);
        mvc.setViewName("index");

        return mvc;
    }

    //after user hit search, this will reflect next or prev page
    @PostMapping(path="/quicksearch", params="page")
    public ModelAndView getPage(@RequestBody MultiValueMap<String, String> formData, 
        @RequestParam(name="page") String page) {
        
        ModelAndView mvc = new ModelAndView();

        String searchTerm = formData.getFirst("searchValue");
        String searchTermForSQL = "%" + searchTerm + "%";
        
        Integer pageInt = Integer.parseInt(page);
        Integer offset = 0 + 10 * (pageInt - 1);
        
        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 10, offset);
        Integer noOfResults = appSvc.getNumberOfResults(searchTermForSQL);

        mvc.addObject("message", "");
        mvc.addObject("fileName", "");
        
        mvc.addObject("resultList", addResultsList);
        mvc.addObject("searchValue", searchTerm);
        mvc.addObject("noOfResults", noOfResults);
        mvc.addObject("page", pageInt);
        mvc.setViewName("index");

        return mvc;
    }
}