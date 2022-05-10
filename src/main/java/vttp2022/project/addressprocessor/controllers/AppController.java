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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import vttp2022.project.addressprocessor.Utils.ReadCsvUtil;
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

    @GetMapping(path={""})
    public ModelAndView getIndex() 
    {       
        ModelAndView mvc = new ModelAndView("index");

        mvc.addObject("above20searches", "");
        mvc.addObject("resultList", Collections.emptyList());
        mvc.addObject("searchValue", "");
        mvc.addObject("searchBy", "");
        mvc.addObject("noOfResults", "");
        mvc.addObject("page", 1);
        mvc.addObject("totalPage", "");
        return mvc;
    }

    @GetMapping(path="/help")
    public ModelAndView getHelp() 
    {
        return new ModelAndView("help");
    }

    //upload queries One Map API
    @PostMapping(path="/upload")
    public ModelAndView postUpload(@RequestParam("csv-file") MultipartFile file,
        @RequestPart(required = false) String toEmail) 
    {
        ModelAndView mvc = new ModelAndView();

        //application/vnd.ms-excel, text/csv - .csv
        //if the file is empty or if the file uploaded is not .csv format
        if(!file.getContentType().equals("application/vnd.ms-excel") 
            && !file.getContentType().equals("text/csv") || file.isEmpty()) 
        {
            mvc.addObject("message", "invalidfiletype");
            mvc.setViewName("error");
            return mvc;
        } else {
            try {
                Set<String> searchValSet = ReadCsvUtil.parseSearchValue(file);
                if (searchValSet.isEmpty()) {
                    //if its empty, means address could not be found in column headers
                    mvc.addObject("message", "addressnotincolheader");
                    mvc.setViewName("error");
                    return mvc;    
                } else if (searchValSet.size() < 20) {
                    //safe to query onemap without @Async
                    String fileName = appSvc.queryOneMapAPI(searchValSet);

                    if (fileName.isBlank()) { // a WriteToByteArrayException occurred, failed to write to DO
                        mvc.addObject("message", "wtbae");
                        mvc.setViewName("error");
                        return mvc;    
                    } else {
                        mvc.addObject("fileName", fileName);
                        mvc.setViewName("download");
                        return mvc;            
                    }
                } else if (searchValSet.size() >= 20 && null == toEmail) {
                    //if there is more than 20 queries to be made, better to have file sent to email instead
                    mvc.addObject("above20searches", "yes");
                    mvc.addObject("resultList", Collections.emptyList());
                    mvc.addObject("searchValue", "");
                    mvc.addObject("searchBy", "");
                    mvc.addObject("noOfResults", "");
                    mvc.addObject("page", 1);
                    mvc.addObject("totalPage", "");
                    mvc.setViewName("index");
                    return mvc;
                } else if (searchValSet.size() >= 20 && null != toEmail) {
                    //this method is @Async because Heroku has a request timeout of 30s
                    appSvc.queryOneMapAPIAsync(searchValSet, toEmail);
                    mvc.setViewName("email");
                    return mvc;        
                }
                return mvc;
            } catch (IOException ioe) {
                mvc.addObject("message", "ioe");
                mvc.setViewName("error");
                return mvc;
            } 
        }
    }

    //quicksearch queries the database
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
        
        String searchTerm = "";
        if (!formData.getFirst("searchValue").isBlank()) {
            searchTerm = formData.getFirst("searchValue");
        } else if (formData.getFirst("searchValue").isBlank() && 
            !formData.getFirst("currSearchValue").isBlank()) 
        {
            searchTerm = formData.getFirst("currSearchValue");
        }

        String searchBy = formData.getFirst("searchBy");
        System.out.println(searchTerm);
        String searchTermForSQL = "%" + searchTerm + "%";
            
        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 10, offset, searchBy);
        Integer noOfResults = appSvc.getNumberOfResults(searchTermForSQL, searchBy);
        int totalPage = (int)Math.ceil(noOfResults/10.0);
        System.out.println(totalPage);
        mvc.addObject("above20searches", "");
        mvc.addObject("resultList", addResultsList);
        mvc.addObject("searchValue", searchTerm);
        mvc.addObject("searchBy", searchBy);
        mvc.addObject("noOfResults", noOfResults);
        mvc.addObject("page", pageInt);
        mvc.addObject("totalPage", totalPage);
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

    @PostMapping(path="/sendemail")
    public ModelAndView postSendEmailTest(@RequestBody MultiValueMap<String, String> formData) {
        try {
            emailSvc.sendEmailWithAttachment(formData.getFirst("toEmail"), formData.getFirst("fileName"));
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return new ModelAndView("email");
    }

}