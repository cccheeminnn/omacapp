package vttp2022.project.addressprocessor.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

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
import vttp2022.project.addressprocessor.services.EmailService;

@Controller
@RequestMapping(path="")
public class AppController {

    @Autowired private AppService appSvc;
    
    @Autowired private DigitalOceanService doSvc;

    @Autowired private EmailService emailSvc;

    @PostMapping(path="/sendemail")
    public ModelAndView postSendEmailTest(@RequestBody MultiValueMap<String, String> formData) {
        System.out.println("attempting to send email");
        try {
            emailSvc.sendEmailWithAttachment(formData.getFirst("toEmail"), formData.getFirst("fileName"));
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return new ModelAndView("redirect:/");
    }

    @GetMapping(path={"", "/upload"})
    public ModelAndView getIndex() {       

        ModelAndView mvc = new ModelAndView();

        mvc.addObject("message", "");
        mvc.addObject("fileName", "");

        mvc.addObject("resultList", Collections.emptyList());
        mvc.addObject("searchValue", "");
        mvc.addObject("searchBy", "");
        mvc.addObject("noOfResults", "");
        mvc.addObject("page", 1);
        mvc.setViewName("index");
        return mvc;

    }

    @GetMapping(path="/help")
    public ModelAndView getHelp() {
        ModelAndView mvc = new ModelAndView("help");

        return mvc;
    }

    @PostMapping(path="/upload")
    public ModelAndView postUpload(@RequestParam("csv-file") MultipartFile file) {
        
        ModelAndView mvc = new ModelAndView();

        //application/vnd.ms-excel - .csv
        System.out.println("uploaded file contentType is " + file.getContentType());
        //if the file is empty or if the file uploaded is not .csv format
        if(file.isEmpty() || !file.getContentType().equals("application/vnd.ms-excel") || !file.getContentType().equals("text/csv")) {
            
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
                mvc.addObject("searchBy", "");
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

    @PostMapping(path="/quicksearch")
    public ModelAndView postPage(@RequestBody MultiValueMap<String, String> formData) {

        ModelAndView mvc = new ModelAndView();
        
        String page = formData.getFirst("page");
        Integer pageInt = 0;
        if (null == page) {
            pageInt = 1;
        } else {
            pageInt = Integer.parseInt(formData.getFirst("page"));

        }
        Integer offset = 0 + 10 * (pageInt - 1);
        
        String searchBy = formData.getFirst("searchBy");
        System.out.println("searching by>>>>>" + searchBy);
        String searchTerm = formData.getFirst("searchValue");
        String searchTermForSQL = "%" + searchTerm + "%";
            
        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 10, offset, searchBy);
        Integer noOfResults = appSvc.getNumberOfResults(searchTermForSQL, searchBy);
        System.out.println("number of results found: " + noOfResults);
        mvc.addObject("message", "");
        mvc.addObject("fileName", "");
        
        mvc.addObject("resultList", addResultsList);
        mvc.addObject("searchValue", searchTerm);
        mvc.addObject("searchBy", searchBy);
        mvc.addObject("noOfResults", noOfResults);
        mvc.addObject("page", pageInt);
        mvc.setViewName("index");

        return mvc;
    }

    @PostMapping(path="/downloadsearchresults")
    public ModelAndView postDownloadSearchResults(@RequestBody MultiValueMap<String, String> formData) {

        ModelAndView mvc = new ModelAndView();

        String searchBy = formData.getFirst("searchBy");
        String searchTerm = formData.getFirst("searchValue");
        String searchTermForSQL = "%" + searchTerm + "%";
            
        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 150000, 0, searchBy);
        try {
            String filename = doSvc.writeToByteArray(addResultsList);
            mvc.addObject("fileName", filename);
            mvc.setViewName("download");
            return mvc;
        } catch (WriteToByteArrayException wtbae) {
            mvc.addObject("message", "wtbae");
            mvc.setViewName("error");
            return mvc;
        }
    }
}