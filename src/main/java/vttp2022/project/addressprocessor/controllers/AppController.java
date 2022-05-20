package vttp2022.project.addressprocessor.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import vttp2022.project.addressprocessor.exceptions.UserAlreadyExistException;
import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.AddressResult;
import vttp2022.project.addressprocessor.models.File;
import vttp2022.project.addressprocessor.services.AppService;
import vttp2022.project.addressprocessor.services.DigitalOceanService;
import vttp2022.project.addressprocessor.services.EmailService;
import vttp2022.project.addressprocessor.services.UserService;
import vttp2022.project.addressprocessor.utils.ReadCsvUtil;

@Controller
@RequestMapping(path="")
public class AppController {
    
    private static final Logger logger 
        = LoggerFactory.getLogger(AppController.class);

    @Autowired private AppService appSvc;
    
    @Autowired private DigitalOceanService doSvc;

    @Autowired private EmailService emailSvc;

    @Autowired private UserService userSvc;

    @GetMapping(path={"/", "/login"})
    public ModelAndView getIndex(HttpSession httpSesh) 
    {
        logger.info("httpSesh username: " + httpSesh.getAttribute("username"));      

        ModelAndView mvc = new ModelAndView();
        if (null == httpSesh.getAttribute("username")) {
            mvc.addObject("register", "");
            mvc.setViewName("login");
            return mvc;
        } else {
            mvc.addObject("above20searches", "");
            mvc.addObject("userEmail", "");
            mvc.addObject("resultList", Collections.emptyList());
            mvc.addObject("searchValue", "");
            mvc.addObject("searchBy", "");
            mvc.addObject("noOfResults", "");
            mvc.addObject("page", 1);
            mvc.addObject("totalPage", "");
            mvc.setViewName("index");
            return mvc;
        }
    }
    
    @GetMapping(path="/user/profile")
    public ModelAndView getProfile(HttpSession httpSesh) {
        ModelAndView mvc = new ModelAndView();
        String email = (String)httpSesh.getAttribute("username");

        List<File> userFileList = userSvc.retrieveUserFiles(email);
        mvc.addObject("username", email);
        mvc.addObject("userfilelist", userFileList);
        mvc.setViewName("profile");
        return mvc;
    }

    @GetMapping(path="/user/logout")
    public ModelAndView getLogout(HttpSession httpSesh) {
        httpSesh.invalidate();
        return new ModelAndView("redirect:/");
    }

    @GetMapping(path="/register")
    public ModelAndView getRegister() {
        return new ModelAndView("register");
    }

    @GetMapping(path="/help")
    public ModelAndView getHelp(HttpSession httpSesh) {
        ModelAndView mvc = new ModelAndView("help");
        
        String username = (String)httpSesh.getAttribute("username");
        if (null != username) {
            mvc.addObject("login", "yes");
        } else {
            mvc.addObject("login", "");
        }

        return mvc;
    }

    @GetMapping(path="/user/quicksearch")
    public ModelAndView getQuicksearches(@RequestParam String searchValue, 
        @RequestParam String searchBy, @RequestParam String page) {
        logger.info("searchValue: " + searchValue + " searchBy: " + searchBy + " page: " + page);
        ModelAndView mvc = new ModelAndView();
            
        String sqlSearchValue = "%" + searchValue + "%";
        int pageInt = Integer.parseInt(page);
        Integer offset = 0 + 10 * (pageInt - 1);

        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(sqlSearchValue, 10, offset, searchBy);
        logger.info("addResultsList size: " + addResultsList.size());
        int noOfResults = appSvc.getNumberOfResults(sqlSearchValue, searchBy);
        int totalPage = (int)Math.ceil(noOfResults/10.0);

        mvc.addObject("above20searches", "");
        mvc.addObject("userEmail", "");
        mvc.addObject("resultList", addResultsList);
        mvc.addObject("searchValue", searchValue);
        mvc.addObject("searchBy", searchBy);
        mvc.addObject("noOfResults", noOfResults);
        mvc.addObject("page", pageInt);
        mvc.addObject("totalPage", totalPage);
        mvc.setViewName("index");

        return mvc;
    }

    //after pressing login
    @PostMapping(path="/login")
    public ModelAndView postLogin(HttpSession httpsesh, @RequestBody MultiValueMap<String, String> formData) {
        ModelAndView mvc = new ModelAndView();
        String email = formData.getFirst("loginEmail");
        String password = formData.getFirst("loginPassword");
        boolean loginSuccess = userSvc.validateLogin(email, password);

        if (loginSuccess) {
            httpsesh.setAttribute("username", email);
            mvc.setViewName("redirect:/");
            return mvc;
        } else {
            mvc.addObject("message", "loginerror");
            mvc.setViewName("error");
            return mvc;
        }
    }

    @PostMapping(path="/register")
    public ModelAndView postRegister(@RequestBody MultiValueMap<String, String> formData) {
        ModelAndView mvc = new ModelAndView();

        String email = formData.getFirst("loginEmail");
        String password = formData.getFirst("loginPassword");
        try {
            boolean registerSuccess = userSvc.registerUser(email, password);
            System.out.println("registerSuccess>>>>>" + registerSuccess);
            if (registerSuccess) {
                mvc.addObject("register", "success");
                mvc.setViewName("login");
                return mvc;
            } else {
                mvc.setViewName("error");
                return mvc;    
            }
            
        } catch (UserAlreadyExistException uaee) {
            mvc.addObject("message", "useralreadyexist");
            mvc.setViewName("error");
            return mvc;
        }
    }

    //upload queries One Map API
    @PostMapping(path="/user/upload")
    public ModelAndView postUpload(@RequestParam("csv-file") MultipartFile file,
        @RequestPart(required = false) String toEmail, HttpSession httpSesh) 
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
            String email = (String)httpSesh.getAttribute("username");
            String queryName = file.getOriginalFilename();

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
                        userSvc.saveUserFile(email, queryName, fileName);
                        mvc.addObject("fileName", fileName);
                        mvc.addObject("userEmail", email);
                        mvc.setViewName("download");
                        return mvc;            
                    }
                } else if (searchValSet.size() >= 20 && null == toEmail) {
                    //if there is more than 20 queries to be made, better to have file sent to email instead
                    mvc.addObject("above20searches", "yes");
                    mvc.addObject("userEmail", email);
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
                    logger.info("to send results to %s".formatted(toEmail));
                    appSvc.queryOneMapAPIAsync(searchValSet, toEmail, queryName);
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
    // @PostMapping(path="/user/quicksearch")
    // public ModelAndView postPage(@RequestBody MultiValueMap<String, String> formData) {
    //     ModelAndView mvc = new ModelAndView();
        
    //     String page = formData.getFirst("page");
    //     Integer pageInt = 0;
    //     if (null == page) {
    //         pageInt = 1;
    //     } else {
    //         pageInt = Integer.parseInt(formData.getFirst("page"));

    //     }
    //     Integer offset = 0 + 10 * (pageInt - 1);
        
    //     String searchTerm = "";
    //     if (!formData.getFirst("searchValue").isBlank()) {
    //         searchTerm = formData.getFirst("searchValue");
    //     } else if (formData.getFirst("searchValue").isBlank() && 
    //         !formData.getFirst("currSearchValue").isBlank()) 
    //     {
    //         searchTerm = formData.getFirst("currSearchValue");
    //     }

    //     String searchBy = formData.getFirst("searchBy");
    //     logger.info("search term is" + searchTerm);
    //     String searchTermForSQL = "%" + searchTerm + "%";
            
    //     List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 10, offset, searchBy);
    //     Integer noOfResults = appSvc.getNumberOfResults(searchTermForSQL, searchBy);
    //     int totalPage = (int)Math.ceil(noOfResults/10.0);

    //     mvc.addObject("above20searches", "");
    //     mvc.addObject("userEmail", "");
    //     mvc.addObject("resultList", addResultsList);
    //     mvc.addObject("searchValue", searchTerm);
    //     mvc.addObject("searchBy", searchBy);
    //     mvc.addObject("noOfResults", noOfResults);
    //     mvc.addObject("page", pageInt);
    //     mvc.addObject("totalPage", totalPage);
    //     mvc.setViewName("index");

    //     return mvc;
    // }

    @PostMapping(path="/user/downloadsearchresults")
    public ModelAndView postDownloadSearchResults(HttpSession httpSesh,
        @RequestBody MultiValueMap<String, String> formData) 
    {   
        ModelAndView mvc = new ModelAndView();
        String email = (String)httpSesh.getAttribute("username");

        String searchBy = formData.getFirst("searchBy");
        String searchTerm = formData.getFirst("searchValue");
        String searchTermForSQL = "%" + searchTerm + "%";
            
        List<AddressResult> addResultsList = appSvc.getAddressesFromSearchValue(searchTermForSQL, 150000, 0, searchBy);
        try {
            String filename = doSvc.writeToByteArray(addResultsList);
            userSvc.saveUserFile(email, searchTerm, filename);
            mvc.addObject("fileName", filename);
            mvc.addObject("userEmail", email);
            mvc.setViewName("download");
            return mvc;
        } catch (WriteToByteArrayException wtbae) {
            mvc.addObject("message", "wtbae");
            mvc.setViewName("error");
            return mvc;
        }
    }

    @PostMapping(path="/user/sendemail")
    public ModelAndView postSendEmailTest(@RequestBody MultiValueMap<String, String> formData) {
        try {
            emailSvc.sendEmailWithAttachment(formData.getFirst("toEmail"), formData.getFirst("fileName"));
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (MalformedURLException murle) {
            murle.printStackTrace();
        }
        return new ModelAndView("email");
    }

    @PostMapping(path="/user/profile/delete")
    public ModelAndView postDeleteFile(@RequestBody MultiValueMap<String, String> formData) {
        ModelAndView mvc = new ModelAndView();

        String fileToDelete = formData.getFirst("fileToDelete");
        logger.info("fileToDelete: " + fileToDelete);

        //deleteFile method delete from table then delete file from DO spaces
        boolean repoDelSuccess = userSvc.deleteFile(fileToDelete);
        if (repoDelSuccess) {
            mvc.setViewName("redirect:/user/profile");
            return mvc;
        } else {
            mvc.addObject("deletefailed", "deletefailed");
            mvc.setViewName("error");
            return mvc;
        }
    }

}