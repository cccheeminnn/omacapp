package vttp2022.project.addressprocessor.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.Address;
import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.services.AppService;

@Controller
@RequestMapping(path="")
public class AppController {

    @Autowired private AppService appSvc;
    
    //Get homepage
    @GetMapping(path={"", "/upload"})
    public ModelAndView getIndex() {       
        ModelAndView mvc = new ModelAndView();
        mvc.addObject("message", "");
        mvc.addObject("fileName", "");
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
                
                List<Address> searchValList = appSvc.parseSearchValue(file);
                if (searchValList.isEmpty()) {
                    mvc.addObject("message", "addressnotincolheader");
                    mvc.setViewName("error");
                    return mvc;    
                }

                List<AddressResult> queryResultList = appSvc.queryOneMapAPI(searchValList);

                String filename = appSvc.writeToByteArray(queryResultList);
                if (filename.isBlank()) {
                    mvc.addObject("message", "douploadwentwrong");
                    mvc.setViewName("error");
                    return mvc;    
                }
                
                mvc.addObject("message", "Uploaded");
                mvc.addObject("fileName", filename);
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
}