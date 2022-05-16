package vttp2022.project.addressprocessor.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vttp2022.project.addressprocessor.exceptions.UserAlreadyExistException;
import vttp2022.project.addressprocessor.models.File;
import vttp2022.project.addressprocessor.repositories.FilesRepository;
import vttp2022.project.addressprocessor.repositories.UsersRepository;

//contains all the authentication methods
@Service
public class UserService {
    
    @Autowired private UsersRepository usersRepo;

    @Autowired private FilesRepository filesRepo;

    @Autowired private DigitalOceanService doSvc;

    //user login authentications
    
    public boolean validateLogin(String email, String password) {
        return usersRepo.validateLogin(email, password);
    }

    public boolean registerUser(String email, String password) throws UserAlreadyExistException{
        if (usersRepo.checkUserExist(email)) {
            throw new UserAlreadyExistException();
        }
         
        return usersRepo.registerUser(email, password);
    }
    
    //end user login authentications

    //user file methods
    
    public void saveUserFile(String email, String queryName, String generatedFilename) {
        filesRepo.insertFilenames(email, queryName, generatedFilename);
    }
    
    public List<File> retrieveUserFiles(String email) {
        return filesRepo.retrieveFilenames(email);
    }

    @Transactional
    public boolean deleteFile(String fileToDelete) {
        //delete row from table first
        boolean deleteFromRepo = filesRepo.deleteFileFromTable(fileToDelete);
        //then delete from DO spaces, this method throws RunTimeException if fails
        boolean deleteFromSpaces = doSvc.deleteObjectRequest(fileToDelete);

        if (deleteFromRepo && deleteFromSpaces)
            return true;

        return false;
    }
    
    //end user file methods
}
